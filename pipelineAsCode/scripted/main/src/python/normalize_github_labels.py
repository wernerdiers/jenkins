import requests
import json
import os

labels_to_add = [
    {
        "name": "type: bug",
        "color": "fc2929"
    },
    {
        "name": "type: duplicate",
        "color": "eeeeee"
    },
    {
        "name": "type: enhancement",
        "color": "009800"
    },
    {
        "name": "type: wontfix",
        "color": "eeeeee"
    },
    {
        "name": "type: feature",
        "color": "009800"
    },
    {
        "name": "status: in progress",
        "color": "0052cc"
    },
    {
        "name": "status: in review",
        "color": "0052cc"
    },
    {
        "name": "type: question",
        "color": "bfe5bf"
    }
]

labels_to_delete = ["bug", "duplicate", "enhancement", "help wanted", "invalid", "question", "wontfix"]

headers = {
    "Content-type": "application/json",
    "Accept": "application/vnd.github.v3+json",
    "Authorization": os.environ.get('GITHUB_BASIC_TOKEN')
}

page = 1
while True:
    r = requests.get('https://api.github.com/orgs/gravitee-io/repos?page=' + str(page), headers=headers)
    page += 1
    if len(r.json()) == 0:
        break
    for repo in r.json():
        print("")
        dirname = repo.get('name')
        print("REPOSITORY: ", dirname)
        url = repo.get('url')
        print(url + '/labels')
        for label in labels_to_add:
            post = requests.post(url + '/labels', data=json.dumps(label), headers=headers)
            if post.status_code == 201:
                print("POST " + label['name'] + " " + str(post.status_code))
            else:
                patch = requests.patch(url + '/labels/' + label['name'], data=json.dumps(label), headers=headers)
                print("PATCH " + label['name'] + " " + str(patch.status_code))
        for label in labels_to_delete:
            delete = requests.delete(url + '/labels/' + label, headers=headers)
            print("DEL " + label + " " + str(delete.status_code))
