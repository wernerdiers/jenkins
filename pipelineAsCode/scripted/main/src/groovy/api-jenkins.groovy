#!groovy

import groovy.json.JsonSlurper

node() {
    BUILD_JOB_NAME = "api-jenkins-build-job-parameters"
    CREDENTIALS = "admin"
    PARAM_ONE = "Ruben_pagate_las_facturas"
    PARAM_TWO = "Ruben_botooooon"

    stage('BUILD-JOB'){
        buildJobWithParameter("${BUILD_JOB_NAME}")
    }

    stage('GET-STATUS'){
        jobStatus = getLastJobStatus("${BUILD_JOB_NAME}")
        print "Job status: $jobStatus"
    }

    stage('COPY-JOB'){
       // copyJob("${BUILD_JOB_NAME}-CLONED","${BUILD_JOB_NAME}")
    }

    stage('CREATE-JOB'){
        //createJob("uno")
    }

    stage('DELETE-JOB'){
        deleteJob("${BUILD_JOB_NAME}-CLONED")
    }
}


def getCrumb(){
    def rx = httpRequest httpMode: 'GET',\
            url: "${env.JENKINS_URL}crumbIssuer/api/json",\
            authentication: "${CREDENTIALS}"
    //println('Status: '+ rx.status )
    //println('Response: '+ rx.content)
    def rxJson = new JsonSlurper().parseText(rx.getContent())
    return rxJson['crumb']
}


def getLastJobStatus(jobName) {
    def rx = httpRequest httpMode: 'GET',\
            url: "${env.JENKINS_URL}job/${jobName}/lastBuild/api/json?pretty=true",\
            authentication: "${CREDENTIALS}",\
            ignoreSslErrors: true,\
            validResponseCodes: '100:500',\
            acceptType: 'APPLICATION_JSON',\
            contentType: 'APPLICATION_JSON',\
            outputFile: 'response_file.json',\
            consoleLogResponseBody: true

    println('***Response: ' + rx.content)
    def rxJson = new JsonSlurper().parseText(rx.getContent())
    return rxJson['result']
}


def buildJob(jobName){
    def CRUMB = getCrumb()
    def rx = httpRequest customHeaders: [[name:'Jenkins-Crumb',\
            value: "${CRUMB}"]],\
            httpMode: 'POST',\
            ignoreSslErrors: true,\
            responseHandle: 'LEAVE_OPEN',\
            url: "${env.JENKINS_URL}job/${jobName}/build",\
            authentication: "${CREDENTIALS}",\
            validResponseCodes: '100:500'

    println('Status: '+ rx.status)
    println('Response: '+ rx.content)
    println('All: '+ rx)
    //def rxJson = new JsonSlurper().parseText(rx.getContent())
    return rx.status
}


def buildJobWithParameter(jobName){
    def CRUMB = getCrumb()
    def URL_JOB = ["${env.JENKINS_URL}job/${jobName}/buildWithParameters/api/json?",
                   "PARAM_ONE=${PARAM_ONE}&",
                   "PARAM_TWO=${PARAM_TWO}",
                    "variables.txt=variables.txt"
    ]


    def rx = httpRequest httpMode: 'POST',\
            customHeaders: [[name:'Jenkins-Crumb',value: "${CRUMB}"]],\
            url: URL_JOB.join(""),\
            authentication: "${CREDENTIALS}",\
            validResponseCodes: '100:500',\
            acceptType: 'APPLICATION_JSON',\
            contentType: 'APPLICATION_JSON',\
            ignoreSslErrors: true,\
            responseHandle: 'LEAVE_OPEN',\
            outputFile: 'response_file.json'
    return rx.status
}


def copyJob(newJobName, sourceJobName){
    def CRUMB = getCrumb()
    def URL_JOB = ["${env.JENKINS_URL}createItem?",
                   "name=${newJobName}&",
                   "mode=copy&",
                   "from=${sourceJobName}"
    ]

    def rx = httpRequest httpMode: 'POST',\
            customHeaders: [[name:'Jenkins-Crumb',value: "${CRUMB}"]],\
            url: URL_JOB.join(""),\
            authentication: "${CREDENTIALS}",\
            validResponseCodes: '100:399',\
            ignoreSslErrors: true
}


def createJob(newJobName) {
    def CRUMB = getCrumb()
    def CONFIG_FILE_PATH= "/var/lib/jenkins/templates/config.xml"
    //def CONFIG_FILE = new FileInputStream("${CONFIG_FILE_PATH}")
    def URL_JOB = "${env.JENKINS_URL}createItem?name=${newJobName} "//--data-binary @${CONFIG_FILE_PATH}"

    def rx = httpRequest httpMode: 'POST',\
            customHeaders: [[name:'Jenkins-Crumb',value: "${CRUMB}"],
                [name:'Content-Type',value: 'text/xml'],
                [name:'Filename',value: "@${CONFIG_FILE_PATH}"]
            ],\
            contentType: 'TEXT_HTML',\
            url: "${URL_JOB}",\
            authentication: "${CREDENTIALS}",\
            ignoreSslErrors: true,\
            validResponseCodes: '100:399'
}


def deleteJob(String jobName) {
    def CRUMB = getCrumb()
    def rx = httpRequest httpMode: 'POST',\
            customHeaders: [[name:'Jenkins-Crumb',value: "${CRUMB}"]],\
            url: "${env.JENKINS_URL}job/${jobName}/doDelete",\
            authentication: "${CREDENTIALS}",\
            ignoreSslErrors: true,\
            validResponseCodes: '100:500'
}


def parseConfigFile(configFile){
    def xml = new XmlParser().parse(configFile)
    xml.foo[0].each {
        it.@id = "test2"
        it.value = "test2"
    }
}

/*
    Simple example - sending "String Parameters":

curl -X POST JENKINS_URL/job/JOB_NAME/build \
  --user USER:TOKEN \
  --data-urlencode json='{"parameter": [{"name":"id", "value":"123"}, {"name":"verbosity", "value":"high"}]}'


    Another example - sending a "File Parameter":

curl -X POST JENKINS_URL/job/JOB_NAME/build \
  --user USER:PASSWORD \
  --form file0=@PATH_TO_FILE \
  --form json='{"parameter": [{"name":"FILE_LOCATION_AS_SET_IN_JENKINS", "file":"file0"}]}'

E.g.curl -X POST http://JENKINS_URL/job/JOB_NAME/build  --form file0=@/home/user/Desktop/sample.xml --form json='{"parameter": [{"name":"harness/Task.xml", "file":"file0"}]}'
Please note, in this example, the symbol '@' is important to mention. Also, the path to the file is absolute path.
In order to make this command work, you need to configure your Jenkins job to take a file parameter and 'name' in this command corresponds to 'file location' field in the Jenkins job configuration.

In above example, 'harness' is just a name of folder that may not exist in your workspace, you can write "name":"Task.xml" and it will place the Task.xml at root of your workspace.
Remember that name in this command should match with File location in file parameter in job configuration.


http://10.1.2.10:8080/api/json?tree=jobs[name],views[name,jobs[name]]

 */