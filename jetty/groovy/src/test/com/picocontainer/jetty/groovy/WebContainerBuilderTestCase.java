/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *                                                                           *
 *****************************************************************************/

package com.picocontainer.jetty.groovy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.URL;

import org.eclipse.jetty.util.IO;
import org.junit.After;
import org.junit.Test;
import com.picocontainer.script.groovy.GroovyContainerBuilder;

import com.picocontainer.MutablePicoContainer;
import com.picocontainer.PicoContainer;

public final class WebContainerBuilderTestCase {

//    private final ObjectReference containerRef = new SimpleReference();

    private MutablePicoContainer pico;

    @After public void tearDown() throws Exception {
        if (pico != null) {
            pico.stop();
        }
        //Thread.sleep(2 * 1000);
    }

    @Test public void testCanComposeWebContainerContextAndFilter() throws InterruptedException, IOException {
        Reader script = new StringReader("" +
                "package com.picocontainer.script.groovy\n" +
                "builder = new GroovyNodeBuilder()\n" +
                "builder.setNode(new "+WebContainerBuilder.class.getName()+"())\n" +
                "nano = builder.container {\n" +
                "    component(instance:'Fred')\n" +
                "    component(instance:new Integer(5))\n" +
                // declare the web container
                "    webContainer(port:8080) {\n" +
                "        context(path:'/bar') {\n" +
                "            initParam(name:'a', value:'b')\n" +
                "            filter(path:'/*', class:com.picocontainer.jetty.groovy.DependencyInjectionTestFilter," +
                "                   dispatchers: com.picocontainer.jetty.PicoContext.DEFAULT_DISPATCH) {\n" +
                "               initParam(name:'foo', value:'bau')\n" +
                "            }\n" +
                "            servlet(path:'/foo2', class:com.picocontainer.jetty.groovy.DependencyInjectionTestServlet)\n" +

                "        }\n" +
                "    }\n" +
                // end declaration
                "}\n");

        assertPageIsHostedWithContents(script, "hello Fred Filtered!(int= 5 bau)<b>", "http://localhost:8080/bar/foo2");
    }

    @Test public void testCanComposeWebContainerContextAndServlet() throws InterruptedException, IOException {
        Reader script = new StringReader("" +
                "package com.picocontainer.script.groovy\n" +
                "builder = new GroovyNodeBuilder()\n" +
                "builder.setNode(new "+WebContainerBuilder.class.getName()+"())\n" +
                "nano = builder.container {\n" +
                "    component(instance:'Fred')\n" +
                // declare the web container
                "    webContainer(port:8080) {\n" +
                "        context(path:'/bar') {\n" +
                "            servlet(path:'/foo', class:com.picocontainer.jetty.groovy.DependencyInjectionTestServlet) {\n" +
                "               initParam(name:'foo', value:'bar')\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                // end declaration
                "}\n");

        assertPageIsHostedWithContents(script, "hello Fred bar<null>", "http://localhost:8080/bar/foo");
    }

    @Test public void testCanComposeWebContainerContextAndServletInstance() throws InterruptedException, IOException {
        Reader script = new StringReader("" +
                "package com.picocontainer.script.groovy\n" +
                "builder = new GroovyNodeBuilder()\n" +
                "builder.setNode(new "+WebContainerBuilder.class.getName()+"())\n" +
                "nano = builder.container {\n" +
                // declare the web container
                "    webContainer(port:8080) {\n" +
                "        context(path:'/bar') {\n" +
                "            servlet(path:'/foo', instance:new com.picocontainer.jetty.groovy.DependencyInjectionTestServlet('Fred')) {\n" +
                //"                setFoo(foo:'bar')\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                // end declaration
                "}\n");

        assertPageIsHostedWithContents(script, "hello Fred<null>", "http://localhost:8080/bar/foo");
    }


    @Test public void testCanComposeWebContainerContextWithExplicitConnector() throws InterruptedException, IOException {
        Reader script = new StringReader("" +
                "package com.picocontainer.script.groovy\n" +
                "builder = new GroovyNodeBuilder()\n" +
                "builder.setNode(new "+WebContainerBuilder.class.getName()+"())\n" +
                "nano = builder.container {\n" +
                "    component(instance:'Fred')\n" +
                // declare the web container
                "    webContainer() {\n" +
                "        blockingChannelConnector(host:'localhost', port:8080)\n" +
                "        context(path:'/bar') {\n" +
                "            servlet(path:'/foo', class:com.picocontainer.jetty.groovy.DependencyInjectionTestServlet)\n" +
                "        }\n" +
                "    }\n" +
                // end declaration
                "}\n");

        assertPageIsHostedWithContents(script, "hello Fred<null>", "http://localhost:8080/bar/foo");
    }

    @Test public void testCanComposeWebContainerAndWarFile() throws InterruptedException, IOException {

        File testWar = TestHelper.getTestWarFile();

        Reader script = new StringReader("" +
                "package com.picocontainer.script.groovy\n" +
                "builder = new GroovyNodeBuilder()\n" +
                "builder.setNode(new "+WebContainerBuilder.class.getName()+"())\n" +
                "nano = builder.container {\n" +
                "    component(instance:'Fred')\n" +
                "    component(instance:new Integer(5))\n" +
                "    component(key:StringBuffer.class, instance:new StringBuffer())\n" +
                // declare the web container
                "    webContainer() {\n" +
                "        blockingChannelConnector(host:'localhost', port:8080)\n" +
                "        xmlWebApplication(path:'/bar', warfile:'"+testWar.getAbsolutePath().replace('\\','/')+"')" +
                "    }\n" +
                // end declaration
                "}\n");

        assertPageIsHostedWithContents(script, "hello Fred bar", "http://localhost:8080/bar/foo");
        assertEquals("-contextInitialized", pico.getComponent(StringBuffer.class).toString());
    }

    @Test public void testCanComposeWebContainerContextAndListener() throws InterruptedException, IOException {

        Reader script = new StringReader("" +
                "package com.picocontainer.script.groovy\n" +
                "builder = new GroovyNodeBuilder()\n" +
                "builder.setNode(new "+WebContainerBuilder.class.getName()+"())\n" +
                "nano = builder.container {\n" +
                "    component(class:StringBuffer.class)\n" +
                // declare the web container
                "    webContainer(port:8080) {\n" +
                "        context(path:'/bar') {\n" +
                "            listener(class:com.picocontainer.jetty.groovy.DependencyInjectionTestListener)\n" +
                "        }\n" +
                "    }\n" +
                // end declaration
                "}\n");

        assertPageIsHostedWithContents(script, "", "http://localhost:8080/bar/");

        StringBuffer stringBuffer = pico.getComponent(StringBuffer.class);

        assertEquals("-contextInitialized", stringBuffer.toString());

        pico.stop();
        pico = null;

        assertEquals("-contextInitialized-contextDestroyed", stringBuffer.toString());

    }

    @Test public void testStaticContentCanBeServed() throws InterruptedException, IOException {

        File testWar = TestHelper.getTestWarFile();

		String absolutePath = testWar.getParentFile().getAbsolutePath();
		absolutePath = absolutePath.replace('\\', '/');

        Reader script = new StringReader("" +
                "package com.picocontainer.script.groovy\n" +
                "builder = new GroovyNodeBuilder()\n" +
                "builder.setNode(new "+WebContainerBuilder.class.getName()+"())\n" +
                "nano = builder.container {\n" +
                // declare the web container
                "    webContainer(port:8080) {\n" +
                "        context(path:'/bar') {\n" +
                "            staticContent(path:'"+absolutePath+"')\n" +
                "        }\n" +
                "    }\n" +
                // end declaration
                "}\n");

        assertPageIsHostedWithContents(script, "<html>\n" +
                " <body>\n" +
                "   hello\n" +
                " </body>\n" +
                "</html>", "http://localhost:8080/bar/hello.html");

        //Thread.sleep(1 * 1000);

        pico.stop();
        pico = null;


    }

    @Test public void testStaticContentCanBeServedWithDefaultWelcomePage() throws InterruptedException, IOException {

        File testWar = TestHelper.getTestWarFile();

        String absolutePath = testWar.getParentFile().getAbsolutePath();
        absolutePath = absolutePath.replace('\\', '/');

        Reader script = new StringReader("" +
                "package com.picocontainer.script.groovy\n" +
                "builder = new GroovyNodeBuilder()\n" +
                "builder.setNode(new "+WebContainerBuilder.class.getName()+"())\n" +
                "nano = builder.container {\n" +
                // declare the web container
                "    webContainer(port:8080) {\n" +
                "        context(path:'/bar') {\n" +
                "            staticContent(path:'" + absolutePath + "', welcomePage:'hello.html')\n" +
                "        }\n" +
                "    }\n" +
                // end declaration
                "}\n");

        assertPageIsHostedWithContents(script, "<html>\n" +
                " <body>\n" +
                "   hello\n" +
                " </body>\n" +
                "</html>", "http://localhost:8080/bar/");

        //Thread.sleep(1 * 1000);

        pico.stop();
        pico = null;

    }

    private void assertPageIsHostedWithContents(final Reader script, final String message, final String url) throws InterruptedException, IOException {
        pico = (MutablePicoContainer) buildContainer(script, null, "SOME_SCOPE");
        assertNotNull(pico);

        //Thread.sleep(2 * 1000);
        String actual;
        try {
            actual = IO.toString(new URL(url).openStream());
        } catch (ConnectException e) {
            Thread.sleep(1000);
            actual = IO.toString(new URL(url).openStream());
        } catch (FileNotFoundException e) {
            actual = "";
        }
        PlatformAssert.assertSameExceptCarriageReturns(message, actual);
    }

    private PicoContainer buildContainer(final Reader script, final PicoContainer parent, final Object scope) {
        return new GroovyContainerBuilder(script, getClass().getClassLoader()).buildContainer(parent, scope, true);
    }
}
