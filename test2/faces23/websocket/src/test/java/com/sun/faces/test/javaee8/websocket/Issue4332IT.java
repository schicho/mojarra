/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package com.sun.faces.test.javaee8.websocket;

import static java.lang.System.getProperty;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

@RunWith(Arquillian.class)
public class Issue4332IT {

    @ArquillianResource
    private URL webUrl;
    private WebClient webClient;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return create(ZipImporter.class, getProperty("finalName") + ".war")
                .importFrom(new File("target/" + getProperty("finalName") + ".war"))
                .as(WebArchive.class);
    }

    @Before
    public void setUp() {
        webClient = new WebClient();
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.setJavaScriptTimeout(120000);
    }

    @Test
    public void test() throws Exception {
        webClient.setIncorrectnessListener((o, i) -> {});

        testWebsocketAfterPostback();
    }

    public void testWebsocketAfterPostback() throws Exception {
        HtmlPage page = webClient.getPage(webUrl + "issue4332.xhtml");

        HtmlSubmitInput postback = (HtmlSubmitInput) page.getHtmlElementById("form:postback");
        page = postback.click();
        webClient.waitForBackgroundJavaScript(60000);
        
        String pageSource = page.getWebResponse().getContentAsString();
        assertTrue(pageSource.contains("/jakarta.faces.push/push?"));

        HtmlSubmitInput button = (HtmlSubmitInput) page.getHtmlElementById("form:button");
        assertTrue(button.asText().equals("push"));

        page = button.click();
        webClient.waitForBackgroundJavaScript(60000);
       
        for (int i=0; i<6; i++) {
            try {
                System.out.println("Wait until WS push - iteration #" + i);
                Thread.sleep(1000); // waitForBackgroundJavaScript doesn't wait until the WS push is arrived.

                assertTrue(page.getHtmlElementById("form:button").asText().equals("pushed!"));
                
                break;
            } catch (Error e) {
                e.printStackTrace();
            }
        }
        
    }

    @After
    public void tearDown() {
        webClient.close();
    }

}