#!groovy
 
import groovy.json.JsonSlurper
 
node() {
    def any_success = false
    stage('check'){
        for(jobname in ['test-jenkins']){
            //jobStatus = getJobStatus(jobname)
            jobStatus = buildJobWithParameter(jobname)
            echo "joda joda $jobStatus"
            if(jobStatus == "SUCCESS" || jobStatus == "UNSTABLE" || jobStatus == 201){
                any_success = true
                break
            }else{
                echo "the job failed"
            }
        }
    }
    stage('report') {
        if(!any_success){
            echo "Job has failed"
            httpRequest 'https://jenkins.example.com/sounds/playSound?src=https://jenkins.example.com/userContent/sounds/dead.wav'
        }
    }
}
 
 
def getJobStatus(String jobName){
    def rx = httpRequest url: "http://10.2.2.4:8080/job/${jobName}/lastBuild/api/json", authentication: 'rubenn-jenkins'
    println('Status: '+ rx.status )
    println('Response: '+ rx.content)
    def rxJson = new JsonSlurper().parseText(rx.getContent())
    return rxJson['result']
}
 
 
def buildJob(String jobName){
    def rx = httpRequest url: "http://10.2.2.4:8080/job/${jobName}/build/api/json", authentication: 'rubenn-jenkins',\
                         httpMode: 'POST', acceptType: 'APPLICATION_JSON', contentType: 'APPLICATION_JSON'
    println('Status: '+ rx.status)
    println('Response: '+ rx.content)
    println('All: '+ rx)
    //def rxJson = new JsonSlurper().parseText(rx.getContent())
    return rx.status
}
 
def buildJobWithParameter(String jobName){
 
    def messageBody = ['app_name': openshift, 'project': devops ]
 
    def rx = httpRequest url: "http://10.2.2.4:8080/job/job-parameter/buildWithParameters", authentication: 'rubenn-jenkins',\
                         httpMode: 'POST', acceptType: 'APPLICATION_JSON', contentType: 'APPLICATION_JSON', \
                         requestBody: messageBody
    
    println('Status: '+ rx.status)
    println('Response: '+ rx.content)
    println('All: '+ rx)
    //def rxJson = new JsonSlurper().parseText(rx.getContent())
    return rx.status
}