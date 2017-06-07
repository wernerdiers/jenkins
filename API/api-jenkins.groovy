#!groovy

import groovy.json.JsonSlurper

node() {
    def any_success = false
    BUILD_JOB_NAME = "api-jenkins-build-job"
    JENKINS_IP_PORT = "10.1.2.10:8080"
    CREDENTIALS = "admin"

    stage('check'){
        jobStatus = getJobStatus("${BUILD_JOB_NAME}")
        print "Job status: $jobStatus"

        if(jobStatus == "SUCCESS" || jobStatus == "UNSTABLE" || jobStatus == 201 || jobStatus == 200){
            any_success = true
        }else{
            echo "${${BUILD_JOB_NAME}} FAILED"
        }
    }

    stage('post'){
        //buildJobCrumb("${BUILD_JOB_NAME}")
        buildJobCrumbWithParameter("${BUILD_JOB_NAME}")
    }
}


def getCrumbIssuer(){
    def rx = httpRequest url: "${env.JENKINS_URL}crumbIssuer/api/json", authentication: "${CREDENTIALS}"
    println('Status: '+ rx.status )
    println('Response: '+ rx.content)
    def rxJson = new JsonSlurper().parseText(rx.getContent())
    return rxJson['crumb']
}


def getJobStatus(String jobName) {
    def rx = httpRequest url: "${env.JENKINS_URL}job/${BUILD_JOB_NAME}/lastBuild/api/json",\
            authentication: "${CREDENTIALS}"

    println('Status: ' + rx.status)
    println('Response: ' + rx.content)

    def rxJson = new JsonSlurper().parseText(rx.getContent())
    return rxJson['result']
}


def buildJob(String jobName){
    def rx = httpRequest url: "${env.JENKINS_URL}job/${env.JOB_NAME}/build/api/json",\
            authentication: "${CREDENTIALS}",\
            httpMode: 'POST',\
            acceptType: 'APPLICATION_JSON',\
            contentType: 'APPLICATION_JSON'

    println('Status: '+ rx.status)
    println('Response: '+ rx.content)
    println('All: '+ rx)
    //def rxJson = new JsonSlurper().parseText(rx.getContent())
    return rx.status
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

    def rx = httpRequest customHeaders: [[name:'Jenkins-Crumb',\
            value: "${CRUMB}"]],\
            httpMode: 'POST',\
            ignoreSslErrors: true,\
            responseHandle: 'LEAVE_OPEN',\
            url: "${env.JENKINS_URL}job/${BUILD_JOB_NAME}/build?PARAM_ONE=${PARAM_ONE}&PARAM_TWO=${PARAM_TWO}",\
            authentication: "${CREDENTIALS}",\
            validResponseCodes: '100:500'

    println('Status: '+ rx.status)
    println('Response: '+ rx.content)
    println('All: '+ rx)
    //def rxJson = new JsonSlurper().parseText(rx.getContent())
    return rx.status
}


def buildJobWithParameter(String jobName){

    def messageBody = ['app_name': openshift, 'project': devops ]

    def rx = httpRequest url: "${env.JENKINS_URL}job/job-parameter/buildWithParameters",\
            authentication: "${CREDENTIALS}",\
            httpMode: 'POST',\
            acceptType: 'APPLICATION_JSON',\
            contentType: 'APPLICATION_JSON', \
            requestBody: messageBody

    println('Status: '+ rx.status)
    println('Response: '+ rx.content)
    println('All: '+ rx)
    //def rxJson = new JsonSlurper().parseText(rx.getContent())
    return rx.status
}


PROBAR CON POST http://10.1.2.10:8080/job/api-jenkins-build-job/build?PARAM_ONE=Ruben_pagate_las_facturas&PARAM_TWO=Ruben_botooooon