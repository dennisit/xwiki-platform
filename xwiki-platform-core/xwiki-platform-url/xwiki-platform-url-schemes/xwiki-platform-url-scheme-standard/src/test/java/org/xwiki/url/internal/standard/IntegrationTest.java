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
package org.xwiki.url.internal.standard;

import java.net.URL;
import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.internal.reference.DefaultEntityReferenceValueProvider;
import org.xwiki.model.internal.reference.DefaultReferenceEntityReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.resource.resources.ResourcesResourceReference;
import org.xwiki.resource.skins.SkinsResourceReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLConfiguration;
import org.xwiki.url.internal.DefaultResourceReferenceResolver;
import org.xwiki.url.internal.DefaultResourceTypeResolver;
import org.xwiki.url.internal.DefaultStringResourceTypeResolver;
import org.xwiki.url.internal.GenericResourceReferenceResolver;
import org.xwiki.url.internal.GenericStringResourceTypeResolver;
import org.xwiki.url.internal.standard.resources.ResourcesResourceReferenceResolver;
import org.xwiki.url.internal.standard.skins.SkinsResourceReferenceResolver;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Integration Tests for resolving URLs using the Standard URL Scheme.
 *
 * @version $Id$
 * @since 7.1M1
 */
@ComponentList({
    DefaultResourceTypeResolver.class,
    DefaultResourceReferenceResolver.class,
    GenericResourceReferenceResolver.class,
    StandardExtendedURLResourceTypeResolver.class,
    StandardExtendedURLResourceReferenceResolver.class,
    DefaultStringResourceTypeResolver.class,
    GenericStringResourceTypeResolver.class,
    DomainWikiReferenceExtractor.class,
    PathWikiReferenceExtractor.class,
    DefaultEntityReferenceValueProvider.class,
    DefaultReferenceEntityReferenceResolver.class,
    StandardStringResourceTypeResolver.class,
    ResourcesResourceReferenceResolver.class,
    SkinsResourceReferenceResolver.class
})
public class IntegrationTest
{
    @Rule
    public MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    private ResourceTypeResolver<ExtendedURL> resourceTypeResolver;

    private ResourceReferenceResolver<ExtendedURL> resourceReferenceResolver;

    @BeforeComponent
    public void setUpComponents() throws Exception
    {
        // Isolate from xwiki configuration file
        URLConfiguration urlConfiguration = this.componentManager.registerMockComponent(URLConfiguration.class);
        when(urlConfiguration.getURLFormatId()).thenReturn("standard");

        // Isolate from xwiki configuration file
        StandardURLConfiguration standardURLConfiguration = this.componentManager.registerMockComponent(
            StandardURLConfiguration.class);
        when(standardURLConfiguration.getEntityPathPrefix()).thenReturn("bin");
        when(standardURLConfiguration.getWikiPathPrefix()).thenReturn("wiki");

        // Isolate from xwiki configuration file
        ModelConfiguration modelConfiguration = this.componentManager.registerMockComponent(ModelConfiguration.class);
        when(modelConfiguration.getDefaultReferenceValue(EntityType.WIKI)).thenReturn("xwiki");

        // Isolate from xwiki's model
        WikiDescriptorManager wikiDescriptorManager =
            this.componentManager.registerMockComponent(WikiDescriptorManager.class);

        // For test simplicity consider that Context CM == CM
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);
    }

    @Before
    public void setUp() throws Exception
    {
        this.resourceTypeResolver = this.componentManager.getInstance(
            new DefaultParameterizedType(null, ResourceTypeResolver.class, ExtendedURL.class));
        this.resourceReferenceResolver = this.componentManager.getInstance(
            new DefaultParameterizedType(null, ResourceReferenceResolver.class, ExtendedURL.class));
    }

    @Test
    public void extractResourceReference() throws Exception
    {
        // Entity Resource References
        assertURL("http://localhost:8080/xwiki/bin/view/space/page", EntityResourceReference.TYPE,
            new EntityResourceReference(new DocumentReference("xwiki", "space", "page"), EntityResourceAction.VIEW));
        assertURL("http://localhost:8080/xwiki/wiki/mywiki/view/space/page", new ResourceType("wiki"),
            new EntityResourceReference(new DocumentReference("mywiki", "space", "page"), EntityResourceAction.VIEW));

        // Resources Resource References
        assertURL("http://localhost:8080/xwiki/resources/js/prototype/prototype.js", ResourcesResourceReference.TYPE,
            new ResourcesResourceReference());

        // GWT Resource References
        assertURL("http://localhost:8080/xwiki/resources/js/xwiki/wysiwyg/xwe/MacroService.gwtrpc",
            EntityResourceReference.TYPE, new EntityResourceReference(new DocumentReference("xwiki", "js", "xwiki"),
                new EntityResourceAction("resources")));

        // Skins Resource References
        assertURL("http://localhost:8080/xwiki/skins/flamingo/logo.png", SkinsResourceReference.TYPE,
            new SkinsResourceReference());

    }

    private void assertEntityURL(String url, String expectedType, EntityResourceAction expectedAction,
        DocumentReference expectedReference) throws Exception
    {
        ExtendedURL extendedURL = new ExtendedURL(new URL(url), "xwiki");
        ResourceType resourceType =
            this.resourceTypeResolver.resolve(extendedURL, Collections.<String, Object>emptyMap());
        assertEquals(expectedType, resourceType.getId());

        EntityResourceReference reference = (EntityResourceReference) this.resourceReferenceResolver.resolve(
            extendedURL, resourceType, Collections.<String, Object>emptyMap());
        assertEquals(expectedAction, reference.getAction());
        assertEquals(expectedReference, reference.getEntityReference());
    }

    private void assertURL(String url, ResourceType expectedType, ResourceReference expectedReference) throws Exception
    {
        ExtendedURL extendedURL = new ExtendedURL(new URL(url), "xwiki");
        ResourceType resourceType =
            this.resourceTypeResolver.resolve(extendedURL, Collections.<String, Object>emptyMap());
        assertEquals(expectedType.getId(), resourceType.getId());

        ResourceReference reference = this.resourceReferenceResolver.resolve(
            extendedURL, resourceType, Collections.<String, Object>emptyMap());
        assertEquals(expectedReference, reference);
    }
}
