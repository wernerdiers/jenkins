try {
    currentBuild.result = 'SUCCESS'

    node () {
        stage('Config') {
            //GIT
            git_url = 'http://admin@10.2.2.3:7990/scm/webhooks/in-line.git'
            git_cred = 'wernerd'
            git url: "${git_url}", branch: '**', credentialsId: "${git_cred}"

            //JIRA Plugin
            JIRA_SITE = 'devjira'
            jira_issue_obj = jiraIssueSelector(issueSelector: [$class: 'DefaultIssueSelector'])
            //jira_issue = jira_issue_obj.toString()
            //jira_issue = jira_issue.substring(1, jira_issue.length() - 1)
            jira_issue = "DEVOPS-5"
            print "Working on: " + getSite() + " with issue ID: " + getIssueId()
        }



        stage('Stage') {

            commit_info = getCommitInfo()
            wrap([$class: 'BuildUser']) {
                sh 'echo ${BUILD_USER}'
                sh 'BUILD_USER="werner"'
                sh 'echo ${BUILD_USER}'
                sh 'export BUILD_USER="werner"'
                sh 'echo ${BUILD_USER}'
            }
        }



        stage('JIRA') {

            print getIssueLinkTypes()
            print getTransitionsTypes()

            //Make build fail
           // YOU_SHALL_NOT_PASS
        }
    }
} catch (e) {
    currentBuild.result = 'FAILURE'
    throw e

} finally {
    // #################  POST-BUILD #################
    stage ('Post-build') {

        print "Build number: " + currentBuild.number + ", result: " + currentBuild.result + ", duration: ${currentBuild.duration}"
        jira_comment_body =  """
                     Job ${env.JOB_NAME} with build number: ${env.BUILD_NUMBER} finished with status:
                     *${currentBuild.result}*. \n
                     *Build URL:* ${env.BUILD_URL} \n\n
                     "${commit_info}"
                     """

        newComment = addComment()
        if(newComment.toString().contains("anonymous")){
            jiraEditComment(
                    site: getSite(),
                    idOrKey: getIssueId(),
                    commentId: newComment.data.id,
                    comment: "${jira_comment_body}"
            )
        }

    }
}


def getSite(){
    return "${JIRA_SITE}"
}

def getIssueId(){
    return "${jira_issue}"
}

def getIssue(){
    def issue = jiraGetIssue(idOrKey: getIssueId(), site: getSite())
    return issue
}

def getProjectId(){
    def projectId = getIssue().data.fields.getProjectId.key
    return "${projectId}"
}

def getIssueLinkTypes(){
    def issueLinkTypes = jiraGetIssueLinkTypes site: getSite(), failOnError: false
    return issueLinkTypes.data.toString()
}

def getTransitionsTypes(){
    def transitionsTypes = jiraGetIssueTransitions idOrKey: getIssueId(), site: getSite()
    return transitionsTypes.data.toString()
}

def editTransitionInput(){
    def transitionInput =
        [
            "update"    : [
                    "comment": [
                            [
                                    "add": [
                                            "body": "Bug has been fixed."
                                    ]
                            ]
                    ]
            ],
            "transition": [
                    "id"    : 21,
                    "fields": [
                            "assignee"  : [
                                    "name": "admin"
                            ],
                            "resolution": [
                                    "name": "Done"
                            ]
                    ]
            ]
        ]
}

def getCommitInfo(){
    def commitInfo =sh (
            script: 'git show $(git rev-parse HEAD) --pretty=format:"%n%n%n*Commit:* %h %n*User:* %an (%ae) %n*When:* %ar%n*Comment:* %s %n*Parent hash:* %p %n*Notes:* %N" --stat && echo -n "*Branch:* " &&  git branch --contains $(git rev-parse HEAD)',
            returnStdout: true
    ).trim()
    return commitInfo
}

def setTransitionIssue(){
    jiraTransitionIssue idOrKey: getIssueId(), input: transitionInput, site: getSite()
}

def editNewIssue(){
    //Create Bug
    def newIssue = [fields: [project    : [id: getProjectId()],
                             summary    : "New JIRA Created from Jenkins.",
                             description: "New JIRA Created from Jenkins.",
                             issuetype  : [id: 10004]]]
}

def setNewIssue(){
    response = jiraNewIssue issue: testIssue, site: getSite()
}

def setLinkedIssue(){
    jiraLinkIssues type: "Relates", inwardKey: getIssueId(), outwardKey: response.data.key, site: getSite()
}

def addComment(){
    //Add comment to the issue
    def newComment = jiraAddComment(
            site: getSite(),
            idOrKey: getIssueId(),
            comment: """Job ${env.JOB_NAME} with build number: ${env.BUILD_NUMBER} finished with status:
                     *${currentBuild.result}*. \n
                     *Build URL:* ${env.BUILD_URL} \n\n
                     "${commit_info}"
            """
    )
    return newComment
}