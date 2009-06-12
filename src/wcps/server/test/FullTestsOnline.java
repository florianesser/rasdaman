/*
 * This file is part of PetaScope.
 *
 * PetaScope is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * PetaScope is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with PetaScope. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information please see <http://www.PetaScope.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 *
 * Copyright 2009 Jacobs University Bremen, Peter Baumann.
 */


package wcps.server.test;

//~--- non-JDK imports --------------------------------------------------------

import org.apache.commons.io.FileUtils;

import wcps.server.cli.grammar;
import wcps.server.cli.xml;

//~--- JDK imports ------------------------------------------------------------

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Runs all available testFiles against a deployed version of Petascope,
 * available at some URL. In particular, here are the steps for each test:
 * 1) Convert abstract syntax query to XML query
 * 2) Convert XML query to a RasQL query
 * 3) Send abstract syntax query to PetaScope WCPS
 * 4) Send XML query to PetaScope WCPS
 *
 * Assumes that a testFiles succeeds if the server does not throw or display
 * an error. Saves the intermediate results in a specified path.
 *
 * @author Andrei Aiordachioaie
 */
public class FullTestsOnline
{
//  public String PetascopeURL = "http://localhost:8080/PetaScope/WCPService";
//  public String PetascopeURL = "http://localhost:8080/petascope/wcps/";
	public String PetascopeURL = "http://kahlua.eecs.jacobs-university.de:8080/wcps-new/";

	// Tests are read from this folder
	String folder = "test/testcases-wcps/";
	// How many testFiles we have to run
	int numTests = 0;
	// Files are written in this folder
	String outputFolder = "test/tmp/";
	// How many successes?
	int passCount = 0;
	// message for testFiles
	String[][] errors;
	// success code
	boolean[] ok;
	// partial success codes
	boolean[][] partialOk;
	// queries
	String[] queries;
	String[] testNames;

	public FullTestsOnline()
	{
		// Find out how many grammar testFiles we have to run
		File dir = new File(folder);

		System.out.println("Looking for tests in " + dir.getAbsolutePath() + "\n");
		TestFileFilter filter = new TestFileFilter();

		testNames = dir.list(filter);
		Arrays.sort(testNames);
		numTests = testNames.length;

		ok = new boolean[numTests];
		partialOk = new boolean[5][numTests];
		errors = new String[5][numTests];
		queries = new String[numTests];
	}

	public void printResults()
	{
		HashMap strMap = new HashMap<Boolean, String>();

		strMap.put(true, "ok");
		strMap.put(false, "failed");

		System.out.println("\n \nRESULTS \n");

		for (int i = 0; i < numTests; i++)
		{
			String tname = testNames[i];

			tname = tname.substring(0, tname.length() - 5);

			if ( ok[i] == true )
			{
				System.out.println("*** Test '" + tname + "' ok");
//              System.out.println("\t" + queries[i]);
			}
			else
			{
				System.out.println("*** Test '" + tname + "' FAILED");
				System.out.println("\t * Abstract Syntax -> Xml: " + strMap.get(partialOk[1][i]));
				if ( partialOk[1][i] == false )
					System.out.println("\t\t" + errors[1][i]);
//                System.out.println("\t * Xml -> RasQL: " + strMap.get(partialOk[2][i]));
//                if (partialOk[2][i] == false)
//                  System.out.println("\t\t" + errors[2][i]);
				System.out.println("\t * Running Abstract Syntax query: " + strMap.get(partialOk[3][i]));
				if ( partialOk[3][i] == false )
					System.out.println("\t\t" + errors[3][i]);
				System.out.println("\t * Running XML query: " + strMap.get(partialOk[4][i]));
				if ( partialOk[4][i] == false )
					System.out.println("\t\t" + errors[4][i]);
			}
		}
		System.out.println("\n\nRESULTS\n");
		System.out.println("Tested PetaScope implementation from: " + PetascopeURL);
		System.out.println("Tests succeeded: " + String.valueOf(passCount));
		System.out.println("Tests failed: " + String.valueOf(numTests - passCount));
	}

	public void runAllTests()
	{
		String abstractQuery = "";
		String xmlQuery = "";
		String rasqlQuery = "";
		String tname = "";

        System.out.println("===========================");
        System.out.println("Testing PetaScope implementation @ " + PetascopeURL);
        System.out.println("===========================\n\n");

		for (int i = 0; i < numTests; i++)
		{
			ok[i] = false;
			partialOk[1][i] = false;
			partialOk[2][i] = true;
			partialOk[3][i] = false;
			partialOk[4][i] = false;
			errors[1][i] = "";
			errors[2][i] = "";
			errors[3][i] = "";
			errors[4][i] = "";

			tname = testNames[i];
			tname = tname.substring(0, tname.length() - 5);
            File testFile = new File(folder + testNames[i]);
			System.out.println("--------------------------------");
            System.out.println("Running test '" + tname + "'...");
            System.out.println("--------------------------------");
			// First of all: read file contents
			try
			{
				abstractQuery = FileUtils.readFileToString(testFile);
				queries[i] = abstractQuery;
                System.out.println(abstractQuery);
			}
			catch (IOException e)
			{
				errors[1][i] = "Could not read file " + testFile.getName();

				continue;
			}
			// Step 1: Convert abstract syntax query to XML query, and save files
			try
			{
				xmlQuery = grammar.convertAbstractQueryToXml(abstractQuery);
				// Copy abstract syntax query to output folder
				File abstractFile = new File(outputFolder + tname + ".test");

				FileUtils.writeStringToFile(abstractFile, abstractQuery);
				// Save XML query to a file in output folder
				File xmlFile = new File(outputFolder + tname + ".xml");

				FileUtils.writeStringToFile(xmlFile, xmlQuery);

				partialOk[1][i] = true;
			}
			catch (Exception e)
			{
				errors[1][i] = e.getMessage();
			}
			// Step 2: Convert XML query to RasQL query, and save files
//			try
//			{
//				rasqlQuery = xml.convertXmlToRasql(xmlQuery);
//				// Save XML query to a file in output folder
//				File rasqlFile = new File(outputFolder + tname + ".rasql");
//				FileUtils.writeStringToFile(rasqlFile, rasqlQuery);
//
//				partialOk[2][i] = true;
//			}
//			catch (Exception e)
//			{
//				errors[2][i] = e.getMessage();
//			}
			// Step 3: Send abstract syntax query to PetaScope WCPS
			try
			{
				String err = runOneTest("query", abstractQuery);

				if ( err == null )
					partialOk[3][i] = true;
				else
					errors[3][i] = err;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			// Step 4: Send XML query to PetaScope WCPS
			try
			{
				String err = runOneTest("xml", xmlQuery);

				if ( err == null )
					partialOk[4][i] = true;
				else
					errors[4][i] = err;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			// Wrap up
			ok[i] = partialOk[1][i] && partialOk[2][i] && partialOk[3][i] && partialOk[4][i];
			if ( ok[i] == true )
				passCount++;
		}
	}

	/**
	 * Send an request to the WCPS server.
	 * Returns a message on error and null otherwise.
	 */
	public String runOneTest(String param, String query) throws MalformedURLException, IOException
	{
//      System.out.println("--------------------");
//      System.out.println(query);
//      System.out.println("\t--------------------");

		// connect to the servlet
		URL servlet = new URL(PetascopeURL);
		HttpURLConnection conn = (HttpURLConnection) servlet.openConnection();

		// inform the connection that we will send output and accept input
		conn.setDoInput(true);
		conn.setDoOutput(true);

		// Don't use a cached version of URL connection.
		conn.setUseCaches(false);
		conn.setDefaultUseCaches(false);

		// Send POST request
		conn.setRequestMethod("POST");

		// Specify the content type that we will send binary data
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

		String data = param + "=" + URLEncoder.encode(query);
		DataOutputStream out = new DataOutputStream(conn.getOutputStream());

		out.writeBytes(data);
		out.flush();
		out.close();

		BufferedReader cgiOutput = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line1 = cgiOutput.readLine();
		String line2 = cgiOutput.readLine();
		String line3 = cgiOutput.readLine();

		System.out.println("\t" + line1);
		System.out.println("\t" + line2);
		System.out.println("\t" + line3);

		if ( (line1 != null) && (line2 != null) && (line3 != null) && line2.equals("<h1>An error has occured</h1>") )
		{
			while (cgiOutput.ready())
				System.out.println("\t" + cgiOutput.readLine());
//          System.out.println("Error executing query: ");
			String error = line3.substring(10, line3.length() - 4);

//          System.out.println("\t" + error);
			return error;
		}
		else
			return null;

	}

	public static void main(String args[])
	{
		FullTestsOnline tester = new FullTestsOnline();
        if (args.length == 1)
            tester.PetascopeURL = args[0];

		tester.runAllTests();
		tester.printResults();
	}

	/* Accept all files with extension TEST. */
	private class TestFileFilter implements FilenameFilter
	{
		@Override
		public boolean accept(File dir, String name)
		{
			if ( name.endsWith("test") )
			{
				return true;
			}

			return false;
		}
	}

}
