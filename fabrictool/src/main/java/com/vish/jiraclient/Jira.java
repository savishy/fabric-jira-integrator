package com.vish.jiraclient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.BasicComponent;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.BasicStatus;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.api.domain.util.ErrorCollection;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

/**
 * a common JIRA Rest client. works in any situation where JIRA needs to be used programmatically.
 * <p>
 * References:
 * <ul>
 * <li><a href="https://ecosystem.atlassian.net/wiki/display/JRJC/Home">Home Page</a></li>
 * <li><a href="https://docs.atlassian.com/jira-rest-java-client-api/2.0.0-m31/jira-rest-java-client-api/apidocs/">Javadoc</a></li> 
 * </ul>
 * @author vish
 *
 */
public class Jira {
	boolean DEBUG = false;
	/** Jira Rest Client */
	public static JiraRestClient restClient;
	/** valid issuetypes */
	public List<IssueType> issueTypes = new ArrayList<IssueType>();
	/** valid components */
	public List<BasicComponent> components = new ArrayList<BasicComponent>();
	/** current project */
	public Project project;
	
	/**
	 * constructor. initialize JIRA REST Client. 
	 * <p>
	 * updates current project. gets latest list of components and issuetypes.
	 * @param url
	 * @param u
	 * @param p
	 * @throws Exception 
	 */
	public Jira(String url, String u, String p, String proj) throws Exception {
		AsynchronousJiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
		URI jiraServerUri = null;
		try {
			jiraServerUri = new URI(url);
			restClient = factory.createWithBasicHttpAuthentication(jiraServerUri, u, p);
			//initialize current project
			project = restClient.getProjectClient().getProject(proj).claim();
			//update issuetypes and components
			updateProjectComponents();
			updateProjectIssueTypes();
		} catch (URISyntaxException e) {
			System.err.println("ERR:" + e.getMessage());
		}
	}

	/**
	 * parse JIRA Rest errors and get error message and status.
	 * @param e
	 */
	private void parseJiraRestError(Collection<ErrorCollection> e) {
		Iterator<ErrorCollection> iter = e.iterator();
		while (iter.hasNext()) {
			ErrorCollection coll = iter.next();
			System.err.println("error message:" + coll.getErrorMessages());
			System.err.println("status:" + coll.getStatus());
		}
	}
	
	/**
	 * Retrieve issue using its key
	 * @param key
	 * @return {@code Map<String,String>} containing following keys:<br>
	 * key - issue ID<br>
	 * summary - issue summary<br>
	 * description<br>
	 * status - status of issue<br>
	 * @throws Exception
	 */
	public Map<String,String> getIssueByKey(String key) throws Exception {
		System.out.println("getIssue() " + key);
		Map<String,String> issueFields = new HashMap<String,String>();
		try {
			Issue issue = restClient.getIssueClient().getIssue(key).claim();
			issueFields.put("key",issue.getKey());
			issueFields.put("summary",issue.getSummary());
			issueFields.put("description",issue.getDescription());
			BasicStatus status = issue.getStatus();
			issueFields.put("status",status.toString());
			System.out.println(issueFields.toString());
			return issueFields;
		} catch (RestClientException e) {
			parseJiraRestError(e.getErrorCollections());
			throw new Exception ("error fetching issue");
		}
	}

	/**
	 * feed in a valid JQL and get issues as a Map.
	 * @param jql
	 * @return {@code Map<String,String>} containing following keys:<br>
	 * key - issue ID<br>
	 * values:String[summary,description,status]<br>
	 * summary - issue summary<br>
	 * description<br>
	 * status - status of issue<br>
	 * @throws Exception
	 */
	public Map<String,String[]> getIssuesByJQL(String jql) throws Exception {
		if (DEBUG) System.out.println("JQL:" + jql);
		SearchResult result = restClient.getSearchClient().searchJql(jql).claim();
		Map<String,String[]> issueFields = new HashMap<String,String[]>();
		if (DEBUG) System.out.println(result.getTotal() + " results");
		if (result.getTotal() > result.getMaxResults())
			System.out.println("WARNING: " + result.getTotal() + " results, printing only first " + result.getMaxResults());
		Iterator<Issue> iter = result.getIssues().iterator();
		
		while (iter.hasNext()) {
			Issue issue = iter.next();
			BasicStatus status = issue.getStatus();
			issueFields.put(issue.getKey(), new String[]{
					issue.getSummary(),
					issue.getDescription(),
					status.toString()
						});
		}
		return issueFields;
	}
	
	private void updateProjectIssueTypes() throws Exception {
		Iterator<IssueType> iter = project.getIssueTypes().iterator();
		while (iter.hasNext()) {
			issueTypes.add(iter.next());
		}
	}
	
	private void updateProjectComponents() throws Exception {
		Iterator<BasicComponent> iter = project.getComponents().iterator();
		while (iter.hasNext()) {
			components.add(iter.next());
		}
	}
	
	/**
	 * get {@link IssueType} object from string.
	 * @param p {@link Project} object referencing current JIRA project.
	 * @param issueType the issue-type as a string e.g "Bug". Case-insensitive.
	 * @return
	 * @throws Exception
	 */
	private IssueType getIssueTypeFromString(Project p, String issueType) throws Exception {
		for (IssueType it : issueTypes) {
			if (it.getName().toLowerCase().equals(issueType.toLowerCase())) {
				return it;
			}
		}
		return (IssueType) null;
	}
	
	private BasicComponent getComponentFromString(Project p, String comp) throws Exception {
		for (BasicComponent it : components) {
			if (it.getName().toLowerCase().equals(comp.toLowerCase())) {
				return it;
			}
		}
		return (BasicComponent) null;
	}
	
	/**
	 * Create issue in the current project. 
	 * @param issueType a valid issue type. 
	 * @param component valid component. 
	 * @param summary issue summary
	 * @param description issue description
	 * @throws Exception
	 */
	public void createIssue(String issueType, 
							String component,
							String summary,
							String description) throws Exception {
		System.out.println("createIssue(): " + project + " " + issueType);
		IssueType it = getIssueTypeFromString(project, issueType);
		BasicComponent comp = getComponentFromString(project, component);
		
		//error handling
		if (it == null) throw new Exception ("invalid issue type: " + issueType + 
				". valid types are:" + Arrays.toString(issueTypes.toArray()));
		if (comp == null) throw new Exception ("invalid component: " + component + 
				". valid values are:" + Arrays.toString(components.toArray()));
		
		IssueInputBuilder builder = new IssueInputBuilder(project, it);
		IssueInput issueInput = builder	
									.setComponents(comp)
									.setDescription(description)
									.setSummary(summary)
									.build()
										;
		BasicIssue issue = restClient.getIssueClient().createIssue(issueInput).claim();
		System.out.println(issue.getKey() + " created");		
	}
}
