package com.vish.fabrictool.fabrictool;

import java.io.File;
import java.io.FileInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.vish.jiraclient.Jira;

/**
 * Hello world!
 *
 */
public class Fabric 
{

	private String FABRIC_URL, FABRIC_LOGINENDPOINT, FABRIC_USERNAME, FABRIC_PWD, FABRIC_OSNAME, FABRIC_ORG, FABRIC_APPPACKAGE;
	private String JIRA_USERNAME, JIRA_PWD, JIRA_URL, JIRA_PROJECT, JIRA_ISSUECOMP, JIRA_ISSUETYPE;
	private String FABRIC_ISSUELINKPREFIX;
	private String CHROME_DRIVER;
	private static boolean DEBUG = false;
	protected WebDriver driver;

	/**
	 * map containing following mapping:
	 * fabric issue number -> String[line,
					url,
					version,
					other details]
	 */
	private Map<String,String[]> issueDetails = new java.util.HashMap<String,String[]>();
	public Fabric() throws Exception {
		Properties config = new Properties();
		config.load(new FileInputStream(new File(System.getProperty("user.dir") + File.separator + "config.properties")));
		JIRA_URL = config.getProperty("jiraurl");
		JIRA_USERNAME = config.getProperty("jirausername");
		JIRA_PWD = config.getProperty("jirapassword");
		JIRA_PROJECT=config.getProperty("jiraproject");
		JIRA_ISSUECOMP=config.getProperty("jiraissuecomponent");
		JIRA_ISSUETYPE=config.getProperty("jiraissuetype"); 
		FABRIC_URL = config.getProperty("fabricurl");
		FABRIC_LOGINENDPOINT = config.getProperty("fabricloginendpoint");
		FABRIC_USERNAME = config.getProperty("fabricusername");
		FABRIC_PWD = config.getProperty("fabricpassword");
		FABRIC_ORG = config.getProperty("fabricorganization");
		FABRIC_APPPACKAGE = config.getProperty("fabricappname");
		FABRIC_OSNAME = config.getProperty("fabricosname");
		FABRIC_ISSUELINKPREFIX = 	"/" + FABRIC_ORG + 
				"/" + FABRIC_OSNAME + 
				"/" + "apps" +
				"/" + FABRIC_APPPACKAGE + 
				"/" + "issues" +
				"/";
		driverInit();
	}


	/**
	 * Get the current date time in a format suitable for printing.
	 * @return current date in format yyyy-MM-dd-HH-mm-s
	 */
	protected static String getDate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		Date date = new Date();
		return(dateFormat.format(date));
	}

	/**
	 * Pretty-prints log messages with datestamp
	 * @param str
	 */
	protected static void doLog (String str) {
		String modStr = "[ " + getDate() + " ]" + str;
		System.out.println(modStr);

	}

	/**
	 * Get path to ChromeDriver.
	 */
	private void getChromeDriver() {
		doLog("OS:" + System.getProperty("os.name"));
		if (System.getProperty("os.name").contains("Windows")) {
			doLog(System.getenv("HOMEDRIVE") + System.getenv("HOMEPATH"));
			CHROME_DRIVER = "D:\\vish\\Google Drive\\tools\\selenium-driver" + File.separator + "chromedriver.exe";			
			doLog("ChromeDriver set to " + CHROME_DRIVER);
		}
	}

	/**
	 * Initialize a ChromeDriver 
	 * @throws Exception
	 */
	private void driverInit() throws Exception {
		getChromeDriver();
		System.setProperty("webdriver.chrome.driver",CHROME_DRIVER);	
		ChromeOptions opts = new ChromeOptions();
		opts.addArguments("--start-maximized");
		driver = new ChromeDriver(opts);
		driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
		driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

	}
	
	/**
	 * login to fabric using Selenium WebDriver
	 * @throws Exception
	 */
	public void fabricLogin() throws Exception {
		driver.get(FABRIC_URL + FABRIC_LOGINENDPOINT); 
		Thread.sleep(2000);
		driver.findElement(By.name("email")).clear();
		driver.findElement(By.name("email")).sendKeys(FABRIC_USERNAME);
		Thread.sleep(2000);
		driver.findElement(By.name("password")).clear();
		driver.findElement(By.name("password")).sendKeys(FABRIC_PWD);
		Thread.sleep(2000);
		driver.findElement(By.cssSelector("button[data-title='Sign In']")).click();
		Thread.sleep(5000);

	}

	/**
	 * retrieve the rows of issues from fabric using WebDriver.
	 * The issues are put into the {@link #issueDetails} HashMap.
	 * @throws Exception
	 */
	public void fabricGetIssueTableRows() throws Exception {		
		//get issue row
		List<WebElement> rows = driver.findElements(By.xpath("//tr[contains(@class,'i_issue')]"));		
		for (WebElement row: rows) {
			//			doLog(row.getText());
			//get each cell of row

			List<WebElement> cells = row.findElements(By.xpath(".//td"));
			if (cells.size() != 7) doLog("WARNING: Fabric issue table has unexpected row count");
			//discard first cell (its a checkbox)
			//second cell is issue ID.
			String issueId = cells.get(1).getText().trim();
			//third cell is issue line no. it has newlines.
			//third cell also contains unique issue URL.
			String line = cells.get(2).getText().replaceAll("\\s+", " ");
			String url = cells.get(2).findElement(By.tagName("a")).getAttribute("href");
			//fourth is version
			String version = cells.get(3).getText().trim();
			//5th,6th,7th contain  info on notes, crashes and users.
			String other = 	FABRIC_URL+ url + "," 
					+ cells.get(4).getText().replaceAll("\\s+", " ") + ","
					+ cells.get(5).getText().replaceAll("\\s+", " ") + ","
					+ cells.get(6).getText().replaceAll("\\s+", " ");
			if (DEBUG) doLog(issueId + ": " + line + " " + version + " " + other);
			issueDetails.put(issueId, new String[]{
					line,
					url,
					version,
					other
			});
		}

	}

	/**
	 * for each issue in {@link #issueDetails} map:
	 * <ul>
	 * <li>Check whether a corresponding issue exists in JIRA.</li>
	 * <li>If yes, no action.</li>
	 * </ul>
	 * @param jira
	 * @throws Exception
	 */
	public Map<String,String[]> searchJiraForExistingFabricIssues(Jira jira) throws Exception {
		Iterator<Entry<String,String[]>> iter = issueDetails.entrySet().iterator();
		Map<String,String[]> crashlyticsIssuesWithNoJira = new HashMap<String,String[]>();
		while (iter.hasNext()) {
			Entry<String,String[]> entry = iter.next();
			String issueFabricUrl = entry.getValue()[1];
			Map<String,String[]> searchResult = jira.getIssuesByJQL("project = " + JIRA_PROJECT + " and component = '" + JIRA_ISSUECOMP + "' and description ~ \"" + issueFabricUrl + "\"");
			String[] issueKeys = searchResult.keySet().toArray(new String[]{""});
			if (issueKeys.length > 0) {
				doLog("Crashlytics Issue: " + entry.getKey() + " JIRA issue: " + Arrays.toString(issueKeys));				
			} else {
				doLog("Crashlytics Issue: " + entry.getKey() + " not mapped to any JIRA issue");
				crashlyticsIssuesWithNoJira.put(entry.getKey(),entry.getValue());
			}
		}
		return crashlyticsIssuesWithNoJira;
	}

	
	/**
	 * Bulk-create issues from input
	 * @param issueDetails
	 * @param jira
	 * @throws Exception
	 */
	public void jiraBulkCreateIssues(Map<String,String[]> issueDetails,Jira jira) throws Exception {
		Iterator<Entry<String,String[]>> iter = issueDetails.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String,String[]> entry = iter.next();
			String fabricIssueNumber = entry.getKey();
			String line = entry.getValue()[0];
			String url = entry.getValue()[1];
			String version = entry.getValue()[2];
			String other = entry.getValue()[3];
			String summary = "[Crashlytics #" + fabricIssueNumber + "] " + line;
			String description = "New Crashlytics issue Found\\\\" + 
								" url:" + url + "\\\\" +
								" version: " + version + "\\\\" +
								" more details: " + other + "\\\\";
								
			jira.createIssue(JIRA_ISSUETYPE, JIRA_ISSUECOMP,summary,description);				

		}
	

	}
	public static void main( String[] args ) throws Exception
	{
		Fabric fabric = new Fabric();
		try {
			Jira jira = new Jira(fabric.JIRA_URL, fabric.JIRA_USERNAME, fabric.JIRA_PWD, fabric.JIRA_PROJECT);
			fabric.fabricLogin();
			fabric.fabricGetIssueTableRows();
			Map<String,String[]> issuesToCreate = fabric.searchJiraForExistingFabricIssues(jira);
			if (issuesToCreate.size() == 0)
				doLog("no issues to create!");
			else {
				fabric.jiraBulkCreateIssues(issuesToCreate,jira);
			}
		} catch (Exception e) {
			throw new Exception (e);
		} finally {
			if (fabric.driver != null) fabric.driver.quit();
			System.exit(0);
		}
	}
}
