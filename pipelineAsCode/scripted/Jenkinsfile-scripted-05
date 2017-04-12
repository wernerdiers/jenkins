node {
    try {
        notifyBuild('STARTED')

        stage('Prepare code') {
            gitCheckThatOut('branch', 'repo URL')
        }

        stage('Testing') {
            echo 'Testing'
            echo 'Testing - publish coverage results'
        }

        stage('Staging') {
            echo 'Deploy Stage'
        }
        // @todo add checkpoint
        stage('Deploy') {
            echo 'Deploy - Backend'
            echo 'Deploy - Frontend'
        }
  } catch (e) {
    // If there was an exception thrown, the build failed
    currentBuild.result = "FAILED"
    throw e
  } finally {
    // Success or failure, always send notifications
    notifyBuild(currentBuild.result)
  }
}


/**
 * Clean a Git project workspace.
 * Uses 'git clean' if there is a repository found.
 * Uses Pipeline 'deleteDir()' function if no .git directory is found.
 */
def gitClean() {
    timeout(time: 60, unit: 'SECONDS') {
        if (fileExists('.git')) {
            echo 'Found Git repository: using Git to clean the tree.'
            // The sequence of reset --hard and clean -fdx first
            // in the root and then using submodule foreach
            // is based on how the Jenkins Git SCM clean before checkout
            // feature works.
            sh 'git reset --hard'
            // Note: -e is necessary to exclude the temp directory
            // .jenkins-XXXXX in the workspace where Pipeline puts the
            // batch file for the 'bat' command.
            //sh 'git clean -fd -e "src/vendor/"'
            //sh 'git submodule foreach --recursive git reset --hard'
            //sh 'git submodule foreach --recursive git clean -ffdx'
        }
        else
        {
            echo 'No Git repository found: using deleteDir() to wipe clean'
            deleteDir()
        }
    }
}

def notifyBuild(String buildStatus = 'STARTED') {
  // build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESSFUL'

  // Default values
  def colorName = 'RED'
  def colorCode = '#FF0000'
  def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
  def summary = "${subject} (${env.BUILD_URL})"
  def details = """<p>STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>"""

  // Override default values based on build status
  if (buildStatus == 'STARTED') {
    color = 'YELLOW'
    colorCode = '#FFFF00'
  } else if (buildStatus == 'SUCCESSFUL') {
    color = 'GREEN'
    colorCode = '#00FF00'
  } else {
    color = 'RED'
    colorCode = '#FF0000'
  }

  // Send notifications
  slackSend (color: colorCode, message: summary)

  emailext(
      subject: subject,
      body: details,
      recipientProviders: [[$class: 'DevelopersRecipientProvider']]
    )
}

def gitCheckThatOut(String branch, String vcsUrl) {
    branch =  branch ?: 'master'
    // cleanup
    gitClean()
    // checkout
    git branch: "${branch}", url: "${vcsUrl}"
    // get last tag
    sh "git describe --abbrev=0 --tags > .git/tagName"
    tagName = readFile('.git/tagName')
    echo "${tagName}"
    // set DisplayName
    currentBuild.displayName = tagName
}
