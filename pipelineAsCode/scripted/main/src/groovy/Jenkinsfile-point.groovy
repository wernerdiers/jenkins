try {
    currentBuild.result = 'SUCCESS'

    node () {
        // #################  CONFIG #################
        stage('Config') {
            //GIT
            git_url = 'http://devbitbucket:7990/scm/escm/jenkins-jira.git'
            git_cred = 'wernerd'
            git url: "${git_url}", branch: '**', credentialsId: "${git_cred}"

            //JIRA Plugin
            JIRA_SITE = 'devjira'
            JIRA_PROJECT = 'default_value'
            jira_issue_obj = jiraIssueSelector(issueSelector: [$class: 'DefaultIssueSelector'])
            jira_issue = jira_issue_obj.toString()
            jira_issue = jira_issue.substring(1, jira_issue.length() - 1)
            print "Working on: ${JIRA_SITE} with issue ID: ${jira_issue}"
        }

        // #################  CHECKOUT #################
        stage('Checkout') {
            //def branch = sh(script: 'rev=$(git rev-parse HEAD); git name-rev $rev | awk \'{ print $2}\'', returnStdout: true)
            sh 'git show $(git rev-parse HEAD) --pretty=format:"%n%n%nThe author of %h was %an (%ae) %nWhen: %ar%nComment: %s %nParent hash: %p %n Notes: %N" --stat && git branch --contains $(git rev-parse HEAD)'
        }

        // #################  ENV VARS #################
        stage('Env Vars') {
            /*
            echo "${env.JIRA_URL}"
            sh 'env > env.txt'
            for (String i : readFile('env.txt').split("\r?\n")) {
                println i
            }
            */
        }

        // #################  JIRA #################
        stage('JIRA') {
            //Get issgue link types
            def issueLinkTypes = jiraGetIssueLinkTypes site: "${JIRA_SITE}", failOnError: false
            echo issueLinkTypes.data.toString()
            
            //Get issue
            def issue = jiraGetIssue(idOrKey: "${jira_issue}", site: "${JIRA_SITE}")
            echo issue.data.toString()
            JIRA_PROJECT = issue.data.fields.project.key

            //Get project
            def project = jiraGetProject(idOrKey: "${JIRA_PROJECT}", site: "${JIRA_SITE}")
            echo project.data.toString()

            //Get transition
            def transitions = jiraGetIssueTransitions idOrKey: "${jira_issue}", site: "${JIRA_SITE}"
            echo transitions.data.toString()

            //Edit transition
            def transitionInput =
                    [
                            "update": [
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
            jiraTransitionIssue idOrKey: "${jira_issue}", input: transitionInput, site: "${JIRA_SITE}"
        }
    }
} catch (e) {
    currentBuild.result = 'FAILURE'
    throw e

} finally {
    // #################  POST-BUILD #################
    stage ('Post-build'){
        print "Build number: " + currentBuild.number + ", result: " + currentBuild.result + ", duration: ${currentBuild.duration}"

        if (currentBuild.result == 'SUCCESS'){
            //Create Bug
            def testIssue = [fields: [ project: [id: 20311],
                                       summary: "New JIRA Created from Jenkins.",
                                       description: "New JIRA Created from Jenkins.",
                                       issuetype: [id: 1]]]

            response = jiraNewIssue issue: testIssue, site: "${JIRA_SITE}"

            echo response.successful.toString()
            echo response.data.toString()
            echo response.data.key
            echo response.data.id

        //Link current issue with new one
        jiraLinkIssues type: "Dependency", inwardKey: "${jira_issue}", outwardKey: response.data.key, site: "${JIRA_SITE}"
        }

        //JIRA Plugin
        /*
        jiraComment (
                issueKey: "${jira_issue}",
                body: "Job name: ${env.JOB_NAME} with build number: ${env.BUILD_NUMBER} finished with status: ${currentBuild.result}. Go to ${env.BUILD_URL} "
        )
        */
        //JIRA-Pipeline-Steps Plugin
        jiraAddComment (
                site: "${JIRA_SITE}",
                idOrKey: "${jira_issue}",
                comment: "Job name: ${env.JOB_NAME} with build number: ${env.BUILD_NUMBER} finished with status: ${currentBuild.result}. Go to ${env.BUILD_URL} "
        )

        //Notify
        def notify = [ subject: "Update about ${jira_issue}",
                       textBody: "Just wanted to update about this issue...",
                       htmlBody: "Just wanted to update about this issue...",
                       to: [ reporter: true,
                             assignee: true,
                             watchers: false,
                             voters: false
                       ]
        ]
       // jiraNotifyIssue idOrKey: "${jira_issue}", notify: notify, site: "${JIRA_SITE}"
    }
}
