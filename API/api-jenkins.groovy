#!groovy

import groovy.json.JsonSlurper

node() {
    BUILD_JOB_NAME = "api-jenkins-build-job-parameters"
    JENKINS_IP_PORT = "10.1.2.10:8080"
    CREDENTIALS = "admin"

    stage('BUILD-JOB'){
        buildJobCrumbWithParameter("${BUILD_JOB_NAME}")
    }

    stage('GET-STATUS'){
        jobStatus = getLastJobStatus("${BUILD_JOB_NAME}")
        print "Job status: $jobStatus"

    }
}


def getCrumbIssuer(){
    def rx = httpRequest httpMode: 'GET',\
            url: "${env.JENKINS_URL}crumbIssuer/api/json",\
            authentication: "${CREDENTIALS}"
    //println('Status: '+ rx.status )
    //println('Response: '+ rx.content)
    def rxJson = new JsonSlurper().parseText(rx.getContent())
    return rxJson['crumb']
}


def getLastJobStatus(String jobName) {
    def rx = httpRequest httpMode: 'GET',\
            url: "${env.JENKINS_URL}job/${BUILD_JOB_NAME}/lastBuild/api/json?pretty=true",\
            authentication: "${CREDENTIALS}",\
            ignoreSslErrors: true,\
            validResponseCodes: '100:500',\
            acceptType: 'APPLICATION_JSON',\
            contentType: 'APPLICATION_JSON',\
            consoleLogResponseBody: true

    println('***Response: ' + rx.content)
    def rxJson = new JsonSlurper().parseText(rx.getContent())
    return rxJson['result']
}


def buildJobCrumb(String jobName){
    def CRUMB = getCrumbIssuer()
    def rx = httpRequest customHeaders: [[name:'Jenkins-Crumb',\
            value: "${CRUMB}"]],\
            httpMode: 'POST',\
            ignoreSslErrors: true,\
            responseHandle: 'LEAVE_OPEN',\
            url: "${env.JENKINS_URL}job/${BUILD_JOB_NAME}/build",\
            authentication: "${CREDENTIALS}",\
            validResponseCodes: '100:500'

    println('Status: '+ rx.status)
    println('Response: '+ rx.content)
    println('All: '+ rx)
    //def rxJson = new JsonSlurper().parseText(rx.getContent())
    return rx.status
}


def buildJobCrumbWithParameter(String jobName){
    def PARAM_ONE = "Ruben_pagate_las_facturas"
    def PARAM_TWO = "Ruben_botooooon"
    def CRUMB = getCrumbIssuer()
    def URL_JOB = ["${env.JENKINS_URL}job/${BUILD_JOB_NAME}/buildWithParameters/api/json?",
                   "PARAM_ONE=${PARAM_ONE}&",
                   "PARAM_TWO=${PARAM_TWO}"
    ]

    def rx = httpRequest httpMode: 'POST',\
            customHeaders: [[name:'Jenkins-Crumb',value: "${CRUMB}"]],\
            ignoreSslErrors: true,\
            responseHandle: 'LEAVE_OPEN',\
            url: URL_JOB.join(""),\
            authentication: "${CREDENTIALS}",\
            validResponseCodes: '100:500',\
            acceptType: 'APPLICATION_JSON',\
            contentType: 'APPLICATION_JSON',\
            outputFile: 'response_file.json'
    return rx.status
}