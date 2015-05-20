/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.mail.internal.thread.context;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiServletResponseStub;
import com.xpn.xwiki.web.XWikiURLFactory;
import com.xpn.xwiki.web.XWikiURLFactoryService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class XWikiContextCopierTest
{
    @Rule
    public MockitoComponentMockingRule<XWikiContextCopier> mocker = new MockitoComponentMockingRule<>(
        XWikiContextCopier.class);

    XWikiServletRequestStub originalRequest;

    XWikiServletResponseStub originalResponse;

    XWikiStoreInterface store;

    XWikiContext original;

    @Before
    public void setup() throws Exception
    {
        Utils.setComponentManager(mocker);

        originalRequest = new XWikiServletRequestStub();
        originalRequest.setHost("host");
        originalRequest.setContextPath("contextPath");
        originalRequest.setScheme("scheme");
        originalRequest.setAttribute("attribute", "value");

        originalResponse = new XWikiServletResponseStub();

        original = new XWikiContext();

        // Set some values
        original.setWikiId("wiki");

        DocumentReference userReference = new DocumentReference("wiki", "Space", "Page");
        EntityReferenceSerializer<String> serializer =
            mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        when(serializer.serialize(userReference)).thenReturn("wiki:Space.Page");

        DocumentReferenceResolver<String> resolver =
            mocker.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");

        original.setUserReference(userReference);

        // Set the mock request & response
        original.setRequest(originalRequest);
        original.setResponse(originalResponse);

        // XWiki mock
        XWiki xwiki = mock(XWiki.class);
        original.setWiki(xwiki);

        // Store mock
        store = mock(XWikiStoreInterface.class);
        when(xwiki.getStore()).thenReturn(store);

        // URL factory mock
        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);

        XWikiURLFactoryService urlFactoryService = mock(XWikiURLFactoryService.class);
        when(urlFactoryService.createURLFactory(Matchers.anyInt(), Matchers.any(XWikiContext.class))).thenReturn(
            urlFactory);
        when(xwiki.getURLFactoryService()).thenReturn(urlFactoryService);
    }

    @Test
    public void copyContext() throws Exception
    {
        XWikiContext copy = mocker.getComponentUnderTest().copy(original);

        // Check that the request is not the same.
        XWikiRequest copiedRequest = copy.getRequest();
        assertNotSame(original.getRequest(), copiedRequest);

        // Check that each value on the cloned request are equal.
        assertEquals(originalRequest.getHeader("x-forwarded-host"), copiedRequest.getHeader("x-forwarded-host"));
        assertEquals(originalRequest.getContextPath(), copiedRequest.getContextPath());
        assertEquals(originalRequest.getScheme(), copiedRequest.getScheme());
        assertEquals(originalRequest.getAttributeNames(), copiedRequest.getAttributeNames());
        assertEquals(originalRequest.getAttribute("attribute"), copiedRequest.getAttribute("attribute"));

        // Check that the response is not the same.
        assertNotSame(originalResponse, copy.getResponse());

        // Check that the context values are cloned.
        assertEquals(original.getUserReference(), copy.getUserReference());
        assertEquals(original.getWikiId(), copy.getWikiId());
        // No URL was present in the original context so a stub is used.
        // Note: for some reason, comparing 2 URLs takes ages (~19 seconds) on my machine. Comparing strings instead for
        // performance.
        assertEquals("http://www.mystuburl.com/", copy.getURL().toString());
        // Actually, all the context keys should be copied.
        assertNotSame(original.entrySet(), copy.entrySet());
        assertEquals(original.entrySet(), copy.entrySet());

        // Some things are not cloned.
        assertSame(original.getWiki(), copy.getWiki());

        // Verify that the store cache has been cleaned for the original context.
        verify(store, times(1)).cleanUp(original);

        // Check that the URLFactory is cloned.
        assertNotNull(copy.getURLFactory());
        assertNotSame(original.getURLFactory(), copy.getURLFactory());
    }
}
