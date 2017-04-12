import os
import re
import shutil
import zipfile
import requests
from shutil import copy2

# Input parameters
version_param = os.environ.get('RELEASE_VERSION')
is_latest_param = True if version_param == "master" else False

# build constants
m2repo_path = '/m2repo'
tmp_path = './tmp/%s' % version_param
policies_path = "%s/policies" % tmp_path
resources_path = "%s/resources" % tmp_path
fetchers_path = "%s/fetchers" % tmp_path
services_path = "%s/services" % tmp_path
reporters_path = "%s/reporters" % tmp_path
repositories_path = "%s/repositories" % tmp_path
snapshotPattern = re.compile('.*-SNAPSHOT')

# BINTRAY
publish_to_bintray = True if os.environ.get('PUBLISH_TO_BINTRAY') == "true" else False
bintray_packages_url = "https://api.bintray.com/packages/gravitee-io/release/distribution"
bintray_content_url = "https://api.bintray.com/content/gravitee-io/release/distribution"
bintray_metadata_url = "https://api.bintray.com/file_metadata/gravitee-io/release"
bintray_headers = {
    "Content-type": "application/json",
    "Authorization": os.environ.get('BINTRAY_BASIC_TOKEN')
}
bintray_upload_headers = {
    "Authorization": os.environ.get('BINTRAY_BASIC_TOKEN')
}



def clean():
    if os.path.exists(tmp_path):
        shutil.rmtree(tmp_path)
    os.makedirs(tmp_path, exist_ok=True)
    os.makedirs(policies_path, exist_ok=True)
    os.makedirs(fetchers_path, exist_ok=True)
    os.makedirs(resources_path, exist_ok=True)
    os.makedirs(services_path, exist_ok=True)
    os.makedirs(reporters_path, exist_ok=True)
    os.makedirs(repositories_path, exist_ok=True)


def get_policies(release_json):
    components = release_json['components']
    search_pattern = re.compile('gravitee-policy-.*')
    policies = []
    for component in components:
        if search_pattern.match(component['name']) and 'gravitee-policy-api' != component['name']:
            policies.append(component)
            if "gravitee-policy-ratelimit" == component['name']:
                policies.append({"name": "gravitee-policy-quota", "version": component['version']})
    return policies


def get_resources(release_json):
    components = release_json['components']
    search_pattern = re.compile('gravitee-resource-.*')
    resources = []
    for component in components:
        if search_pattern.match(component['name']) and 'gravitee-resource-api' != component['name']:
            resources.append(component)
    return resources


def get_fetchers(release_json):
    components = release_json['components']
    search_pattern = re.compile('gravitee-fetcher-.*')
    fetchers = []
    for component in components:
        if search_pattern.match(component['name']) and 'gravitee-fetcher-api' != component['name']:
            fetchers.append(component)
    return fetchers


def get_reporters(release_json):
    components = release_json['components']
    search_pattern = re.compile('gravitee-reporter-.*')
    reporters = []
    for component in components:
        if search_pattern.match(component['name']) and 'gravitee-reporter-api' != component['name']:
            reporters.append(component)
    return reporters


def get_repositories(release_json):
    components_name = [
        "gravitee-repository-mongodb",
        "gravitee-repository-ehcache",
        "gravitee-repository-cassandra",
        "gravitee-repository-redis",
        "gravitee-repository-elasticsearch"
    ]
    repositories = []
    for component_name in components_name:
        repositories.append(get_component_by_name(release_json, component_name))
    return repositories


def get_services(release_json):
    components = release_json['components']
    search_pattern = re.compile('gravitee-policy-ratelimit')
    service = None
    for component in components:
        if search_pattern.match(component['name']):
            service = component.copy()
            break
    service['name'] = 'gravitee-gateway-services-ratelimit'
    return [service]


def get_component_by_name(release_json, component_name):
    components = release_json['components']
    search_pattern = re.compile(component_name)
    for component in components:
        if search_pattern.match(component['name']):
            return component


def get_download_url(group_id, artifact_id, version, t):
    return "%s/%s/%s/%s/%s-%s.%s" % (
        m2repo_path, group_id.replace(".", "/"), artifact_id, version, artifact_id, version, t
    )
    # return "https://oss.sonatype.org/service/local/artifact/maven/redirect?r=%s&g=%s&a=%s&v=%s&e=%s" % (
    #    ("snapshots" if snapshotPattern.match(version) else "releases"), group_id, artifact_id, version, t)


def download(name, filename_path, url):
    print('\nDowloading %s\n%s' % (name, url))
    copy2(url, filename_path)
    # response = requests.get(url, stream=True)
    # content_length = response.headers['Content-Length']
    # with open(filename_path, "wb") as handle:
    #     for data in tqdm(response.iter_content(), leave=True, total=int(content_length)):
    #         handle.write(data)
    return filename_path


def unzip(files):
    unzip_dirs = []
    for file in files:
        with zipfile.ZipFile(file) as zip_file:
            zip_file.extractall(tmp_path + "/dist")
            unzip_dir = "%s/dist/%s" % (tmp_path, sorted(zip_file.namelist())[0])
            unzip_dirs.append(unzip_dir)
            preserve_permissions(unzip_dir)
    return sorted(unzip_dirs)


def preserve_permissions(d):
    search_bin_pattern = re.compile(".*/bin$")
    search_gravitee_pattern = re.compile("gravitee(\.bat)?")
    perm = 0o0755
    for dirname, subdirs, files in os.walk(d):
        if search_bin_pattern.match(dirname):
            for file in files:
                if search_gravitee_pattern.match(file):
                    file_path = "%s/%s" % (dirname, file)
                    print("       set permission %o to %s" % (perm, file_path))
                    os.chmod(file_path, perm)


def copy_files_into(src_dir, dest_dir, exclude_pattern=None):
    if exclude_pattern is None:
        exclude_pattern = []
    filenames = [os.path.join(src_dir, fn) for fn in next(os.walk(src_dir))[2]]

    print("        copy")
    print("            %s" % filenames)
    print("        into")
    print("            %s" % dest_dir)
    for file in filenames:
        to_exclude = False
        for pattern in exclude_pattern:
            search_pattern = re.compile(pattern)
            if search_pattern.match(file):
                to_exclude = True
                break
        if to_exclude:
            print("[INFO] %s is excluded from files." % file)
            continue
        copy2(file, dest_dir)


def download_policies(policies):
    paths = []
    for policy in policies:
        if policy['name'] != "gravitee-policy-core":
            url = get_download_url("io.gravitee.policy", policy['name'], policy['version'], "zip")
            paths.append(
                download(policy['name'], '%s/%s-%s.zip' % (policies_path, policy['name'], policy['version']), url))
    return paths


def download_management_api(mgmt_api):
    url = get_download_url("io.gravitee.management.standalone", "gravitee-management-api-standalone-distribution-zip",
                           mgmt_api['version'], "zip")
    return download(mgmt_api['name'], '%s/%s-%s.zip' % (tmp_path, mgmt_api['name'], mgmt_api['version']), url)


def download_gateway(gateway):
    url = get_download_url("io.gravitee.gateway.standalone", "gravitee-gateway-standalone-distribution-zip",
                           gateway['version'], "zip")
    return download(gateway['name'], '%s/%s-%s.zip' % (tmp_path, gateway['name'], gateway['version']), url)


def download_fetchers(fetchers):
    paths = []
    for fetcher in fetchers:
        url = get_download_url("io.gravitee.fetcher", fetcher['name'], fetcher['version'], "zip")
        paths.append(
            download(fetcher['name'], '%s/%s-%s.zip' % (fetchers_path, fetcher['name'], fetcher['version']), url))
    return paths


def download_resources(resources):
    paths = []
    for resource in resources:
        url = get_download_url("io.gravitee.resource", resource['name'], resource['version'], "zip")
        paths.append(
            download(resource['name'], '%s/%s-%s.zip' % (resources_path, resource['name'], resource['version']), url))
    return paths


def download_services(services):
    paths = []
    for service in services:
        url = get_download_url("io.gravitee.policy", service['name'], service['version'], "zip")
        paths.append(
            download(service['name'], '%s/%s-%s.zip' % (services_path, service['name'], service['version']), url))
    return paths


def download_ui(ui):
    url = get_download_url("io.gravitee.management", ui['name'], ui['version'], "zip")
    return download(ui['name'], '%s/%s-%s.zip' % (tmp_path, ui['name'], ui['version']), url)


def download_reporters(reporters):
    paths = []
    for reporter in reporters:
        url = get_download_url("io.gravitee.reporter", reporter['name'], reporter['version'], "zip")
        paths.append(
            download(reporter['name'],
                     '%s/%s-%s.zip' % (reporters_path, reporter['name'], reporter['version']),
                     url))
    return paths


def download_repositories(repositories):
    paths = []
    for repository in repositories:
        url = get_download_url("io.gravitee.repository", repository['name'], repository['version'], "zip")
        paths.append(
            download(repository['name'],
                     '%s/%s-%s.zip' % (repositories_path, repository['name'], repository['version']),
                     url))
    return paths


def prepare_gateway_bundle(gateway):
    print("==================================")
    print("Prepare %s" % gateway)
    bundle_path = unzip([gateway])[0]
    print("        bundle_path: %s" % bundle_path)
    copy_files_into(policies_path, bundle_path + "plugins")
    copy_files_into(resources_path, bundle_path + "plugins")
    copy_files_into(repositories_path, bundle_path + "plugins", [".*gravitee-repository-elasticsearch.*"])
    copy_files_into(reporters_path, bundle_path + "plugins")
    copy_files_into(services_path, bundle_path + "plugins")


def prepare_ui_bundle(ui):
    print("==================================")
    print("Prepare %s" % ui)
    bundle_path = unzip([ui])[0]
    print("        bundle_path: %s" % bundle_path)


def prepare_mgmt_bundle(mgmt):
    print("==================================")
    print("Prepare %s" % mgmt)
    bundle_path = unzip([mgmt])[0]
    print("        bundle_path: %s" % bundle_path)
    copy_files_into(policies_path, bundle_path + "plugins")
    copy_files_into(resources_path, bundle_path + "plugins")
    copy_files_into(fetchers_path, bundle_path + "plugins")
    copy_files_into(repositories_path, bundle_path + "plugins", [".*gravitee-repository-ehcache.*"])


def prepare_policies(version):
    print("==================================")
    print("Prepare Policies")
    policies_dist_path = "%s/dist/gravitee-policies-%s" % (tmp_path, version)
    os.makedirs(policies_dist_path, exist_ok=True)
    copy_files_into(policies_path, policies_dist_path)
    copy_files_into(services_path, policies_dist_path)


def package(version):
    print("==================================")
    print("Packaging")
    packages = []
    exclude_from_full_zip_list = [re.compile(".*graviteeio-policies.*")]
    full_zip_name = "graviteeio-full-%s" % version
    full_zip_path = "%s/dist/%s.zip" % (tmp_path, full_zip_name)
    dirs = [os.path.join("%s/dist/" % tmp_path, fn) for fn in next(os.walk("%s/dist/" % tmp_path))[1]]
    with zipfile.ZipFile(full_zip_path, "w", zipfile.ZIP_DEFLATED) as full_zip:
        print("Create %s" % full_zip_path)
        packages.append(full_zip_path)
        for d in dirs:
            with zipfile.ZipFile("%s.zip" % d, "w", zipfile.ZIP_DEFLATED) as bundle_zip:
                print("Create %s.zip" % d)
                packages.append("%s.zip" % d)
                dir_abs_path = os.path.abspath(d)
                dir_name = os.path.split(dir_abs_path)[1]
                for dirname, subdirs, files in os.walk(dir_abs_path):
                    exclude_from_full_zip = False
                    for pattern in exclude_from_full_zip_list:
                        if pattern.match(d):
                            exclude_from_full_zip = True
                            break
                    for filename in files:
                        absname = os.path.abspath(os.path.join(dirname, filename))
                        arcname = absname[len(dir_abs_path) - len(dir_name):]
                        bundle_zip.write(absname, arcname)
                        if exclude_from_full_zip is False:
                            full_zip.write(absname, "%s/%s" % (full_zip_name, arcname))
                    if len(files) == 0:
                        absname = os.path.abspath(dirname)
                        arcname = absname[len(dir_abs_path) - len(dir_name):]
                        bundle_zip.write(absname, arcname)
                        if exclude_from_full_zip is False:
                            full_zip.write(absname, "%s/%s" % (full_zip_name, arcname))
    return packages


def rename(string):
    return string.replace("gravitee", "graviteeio") \
        .replace("management-standalone", "management-api") \
        .replace("management-webui", "management-ui") \
        .replace("standalone-", "")


def clean_dir_names():
    print("==================================")
    print("Clean directory names")
    dirs = [os.path.join("%s/dist/" % tmp_path, fn) for fn in next(os.walk("%s/dist/" % tmp_path))[1]]
    for d in dirs:
        os.rename(d, rename(d))


def send_to_bintray(nightlybuild, version, packages):
    print("==================================")
    bintray_version = create_bintray_version(nightlybuild, version)
    for p in packages:
        file = open(p, 'rb')
        url = "%s/%s/%s/%s?publish=1" % (
            bintray_content_url, bintray_version, bintray_version, file.name.rpartition("/")[2])
        print(url)
        r = requests.put(url, data=file.read(), headers=bintray_upload_headers)
        response_pretty_print(r)


def delete_bintray_version(version):
    print("BINTRAY - Delete version %s" % version)
    r = requests.get("%s/versions/%s" % (bintray_packages_url, version), headers=bintray_headers)
    if r.status_code == 404:
        print("          version not exists")
    else:
        r = requests.delete("%s/versions/%s" % (bintray_packages_url, version), headers=bintray_headers)
        response_pretty_print(r)


def get_bintray_version(nightlybuild, version):
    return "nightly" if nightlybuild else version


def create_bintray_version(nightlybuild, version):
    bintray_version = get_bintray_version(nightlybuild, version)
    if nightlybuild:
        delete_bintray_version(bintray_version)

    print("BINTRAY - Create version %s" % version)
    payload = {
        "name": bintray_version,
        "desc": "Gravitee.io distribution version %s" % version
    }

    r = requests.post("%s/versions" % bintray_packages_url, json=payload, headers=bintray_headers)
    response_pretty_print(r)
    return bintray_version


def update_bintray_download_list(nightlybuild, version, packages):
    bintray_version = get_bintray_version(nightlybuild, version)
    full_zip = None
    search_pattern = re.compile('.*graviteeio-full-.*')
    for p in packages:
        if search_pattern.match(p):
            full_zip = p.rpartition("/")[2]
            break
    print("BINTRAY - update download list : %s" % full_zip)
    payload = {"list_in_downloads": True}
    r = requests.put("%s/%s/%s" % (bintray_metadata_url, bintray_version, full_zip), json=payload,
                     headers=bintray_headers)
    response_pretty_print(r)


def response_pretty_print(r):
    print("###########################################################")
    print("STATUS %s" % r.status_code)
    print("HEADERS \n%s" % r.headers)
    print("RESPONSE \n%s" % r.text)
    print("###########################################################\n\n")
    r.raise_for_status()


def main():
    if is_latest_param:
        release_json_url = "https://raw.githubusercontent.com/gravitee-io/release/master/release.json"
    else:
        release_json_url = "https://raw.githubusercontent.com/gravitee-io/release/%s/release.json" % version_param

    print(release_json_url)
    release_json = requests.get(release_json_url)
    print(release_json)
    release_json = release_json.json()
    version = release_json['version']

    print("Create bundles for Gravitee.io v%s" % version)
    clean()

    mgmt_api = download_management_api(get_component_by_name(release_json, "gravitee-management-rest-api"))
    ui = download_ui(get_component_by_name(release_json, "gravitee-management-webui"))
    gateway = download_gateway(get_component_by_name(release_json, "gravitee-gateway"))
    download_policies(get_policies(release_json))
    download_resources(get_resources(release_json))
    download_fetchers(get_fetchers(release_json))
    download_services(get_services(release_json))
    download_reporters(get_reporters(release_json))
    download_repositories(get_repositories(release_json))

    prepare_gateway_bundle(gateway)
    prepare_ui_bundle(ui)
    prepare_mgmt_bundle(mgmt_api)
    prepare_policies(version)

    clean_dir_names()
    packages = package(version)
    if publish_to_bintray:
        send_to_bintray(is_latest_param, version, packages)
        update_bintray_download_list(is_latest_param, version, packages)


main()
