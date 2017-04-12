import requests
import json
import os

headers = {
    "Content-type": "application/json",
    "Accept": "application/vnd.github.v3+json",
    "Authorization": os.environ.get('GITHUB_BASIC_TOKEN')
}

payload = {
    "name": "jenkins",
    "active": True,
    "type": "Repository",
    "events": [
        "push",
        "pull_request"
    ],
    "config": {
        "jenkins_hook_url": "https://ci.gravitee.io/github-webhook/"
    }
}

page = 1
while True:
    r = requests.get('https://api.github.com/orgs/gravitee-io/repos?page=' + str(page), headers=headers)
    page += 1
    print(r)
    if len(r.json()) == 0:
        break
    for repo in r.json():
        print("")
        print(repo)
        print("")
        dirname = repo.get('name')
        print("REPOSITORY: ", dirname)
        url = repo.get('url')
        get = requests.get(url + "/hooks", headers=headers)
        add_jenkins_webhook = True
        for hook in get.json():
            if hook.get('name') == 'jenkins':
                print("Jenkins webhook exists : skip")
                add_jenkins_webhook = False
                break
        if add_jenkins_webhook:
            print("add webhook")
            print(requests.post(url + "/hooks", data=json.dumps(payload), headers=headers))
