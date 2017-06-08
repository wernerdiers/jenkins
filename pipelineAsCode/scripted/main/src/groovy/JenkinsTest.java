import java.util.ArrayList;
import java.util.List;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * API: 
 * http://my.jenkins.com/api/?
 *
 * LIST JOBS:
 * http://my.jenkins.com/api/xml
 *
 * COPY JOB: Create AA_TEST_JOB1 by copying AA_TEST_JOB0
 * curl -H "Content-Type:application/xml" "http://my.jenkins.com/createItem?name=AA_TEST_JOB1&mode=copy&from=AA_TEST_JOB0" 
 *
 * DELETE JOB:
 * curl -X POST "http://my.jenkins.com/job/AA_TEST_JOB1/doDelete"
 *
 * READ JOB:
 * curl "http://my.jenkins.com/job/AA_TEST_JOB0/config.xml" > config.xml
 *
 * CREATE JOB: Create AA_TEST_JOB2 by using xml for configuration
 * curl -X POST -H "Content-Type:application/xml" -d "<project><builders/><publishers/><buildWrappers/></project>" "http://my.jenkins.com/createItem?name=AA_TEST_JOB2"
 * curl -X POST -H "Content-Type:application/xml" -d @config.xml "http://my.jenkins.com/createItem?name=AA_TEST_JOB3" 
 *
 * @author sizu
 *
 */
public class JenkinsTest {
    public static void main(String[] args) {
        System.out.println("Check 0: List Jobs");
        List<String> jobList = listJobs("http://my.jenkins.com");
        System.out.println("First Job:"+jobList.get(0));

        System.out.println("Check 1: Delete AA_TEST_JOB1 (created manually)");
        deleteJob("http://my.jenkins.com", "AA_TEST_JOB1");

        System.out.println("Check 2: Create AA_TEST_JOB2 by copying first job");
        copyJob("http://my.jenkins.com", "AA_TEST_JOB2", jobList.get(0));
//		deleteJob("http://my.jenkins.com", "AA_TEST_JOB2");

        System.out.println("Check 3: Create AA_TEST_JOB3 by using a generic xml configuration");
        createJob("http://my.jenkins.com", "AA_TEST_JOB3", "<project><builders/><publishers/><buildWrappers/></project>");
//		deleteJob("http://my.jenkins.com", "AA_TEST_JOB3");

        System.out.println("Check 4: Create AA_TEST_JOB4 by using the xml configuration from the first job (similar to copyJob)");
        String configXML = readJob("http://my.jenkins.com", jobList.get(0));
        createJob("http://my.jenkins.com", "AA_TEST_JOB4", configXML);
//		deleteJob("http://my.jenkins.com", "AA_TEST_JOB4");
    }

    public static List<String> listJobs(String url) {
        Client client = Client.create();
//		client.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter(USERNAME, PASSWORD));
        WebResource webResource = client.resource(url+"/api/xml");
        ClientResponse response = webResource.get(ClientResponse.class);
        String jsonResponse = response.getEntity(String.class);
        client.destroy();
//		System.out.println("Response listJobs:::::"+jsonResponse);

        // Assume jobs returned are in xml format, TODO using an XML Parser would be better here
        // Get name from <job><name>...
        List<String> jobList = new ArrayList<String>();
        String[] jobs = jsonResponse.split("job>"); // 1, 3, 5, 7, etc will contain jobs
        for(String job: jobs){
            String[] names = job.split("name>");
            if(names.length == 3) {
                String name = names[1];
                name = name.substring(0,name.length()-2); // Take off </ for the closing name tag: </name>
                jobList.add(name);
//				System.out.println("name:"+name);
            }
//			System.out.println("job:"+job);
//			for(String name: names){
//				System.out.println("name:"+name);
//			}
        }
        return jobList;
    }

    public static String deleteJob(String url, String jobName) {
        Client client = Client.create();
//		client.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter(USERNAME, PASSWORD));
        WebResource webResource = client.resource(url+"/job/"+jobName+"/doDelete");
        ClientResponse response = webResource.post(ClientResponse.class);
        String jsonResponse = response.getEntity(String.class);
        client.destroy();
//		System.out.println("Response deleteJobs:::::"+jsonResponse);
        return jsonResponse;
    }

    public static String copyJob(String url, String newJobName, String oldJobName){
        Client client = Client.create();
//		client.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter(USERNAME, PASSWORD));
        WebResource webResource = client.resource(url+"/createItem?name="+newJobName+"&mode=copy&from="+oldJobName);
        ClientResponse response = webResource.type("application/xml").get(ClientResponse.class);
        String jsonResponse = response.getEntity(String.class);
        client.destroy();
//		System.out.println("Response copyJob:::::"+jsonResponse);
        return jsonResponse;
    }

    public static String createJob(String url, String newJobName, String configXML){
        Client client = Client.create();
//		client.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter(USERNAME, PASSWORD));
        WebResource webResource = client.resource(url+"/createItem?name="+newJobName);
        ClientResponse response = webResource.type("application/xml").post(ClientResponse.class, configXML);
        String jsonResponse = response.getEntity(String.class);
        client.destroy();
        System.out.println("Response createJob:::::"+jsonResponse);
        return jsonResponse;
    }

    public static String readJob(String url, String jobName){
        Client client = Client.create();
//		client.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter(USERNAME, PASSWORD));
        WebResource webResource = client.resource(url+"/job/"+jobName+"/config.xml");
        ClientResponse response = webResource.get(ClientResponse.class);
        String jsonResponse = response.getEntity(String.class);
        client.destroy();
//		System.out.println("Response readJob:::::"+jsonResponse);
        return jsonResponse;
    }