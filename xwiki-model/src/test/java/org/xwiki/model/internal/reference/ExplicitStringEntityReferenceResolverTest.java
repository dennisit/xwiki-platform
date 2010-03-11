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
package org.xwiki.model.internal.reference;

import org.junit.Assert;
import org.junit.Before;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;

/**
 * Unit tests for {@link org.xwiki.model.internal.reference.ExplicitStringEntityReferenceResolver}.
 *
 * @version $Id$
 * @since 2.2.3
 */
public class ExplicitStringEntityReferenceResolverTest
{
    private EntityReferenceResolver<String> resolver;

    @Before
    public void setUp() throws Exception
    {
        this.resolver = new ExplicitStringEntityReferenceResolver();
    }

    @org.junit.Test
    public void testResolveWithExplicitDocumentReference()
    {
        EntityReference reference =
            this.resolver.resolve("", EntityType.DOCUMENT, new DocumentReference("wiki", "space", "page"));

        Assert.assertEquals("page", reference.getName());
        Assert.assertEquals(EntityType.DOCUMENT, reference.getType());
        Assert.assertEquals("space", reference.getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getType());
        Assert.assertEquals("wiki", reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getType());
    }

    @org.junit.Test
    public void testResolveWithExplicitEntityReference()
    {
        EntityReference reference =
            this.resolver.resolve("space.page", EntityType.DOCUMENT, new EntityReference("wiki", EntityType.WIKI));

        Assert.assertEquals("page", reference.getName());
        Assert.assertEquals(EntityType.DOCUMENT, reference.getType());
        Assert.assertEquals("space", reference.getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getType());
        Assert.assertEquals("wiki", reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getType());
    }

    @org.junit.Test
    public void testResolveWithAbsoluteReferenceAndNoExplicitReference()
    {
        EntityReference reference = this.resolver.resolve("wiki:space.page", EntityType.DOCUMENT);

        Assert.assertEquals("page", reference.getName());
        Assert.assertEquals(EntityType.DOCUMENT, reference.getType());
        Assert.assertEquals("space", reference.getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getType());
        Assert.assertEquals("wiki", reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getType());
    }

    @org.junit.Test
    public void testResolveWithExplicitReferenceWithHoles()
    {
        EntityReference reference = this.resolver.resolve("space.page", EntityType.DOCUMENT,
            new EntityReference("page", EntityType.DOCUMENT, new EntityReference("wiki", EntityType.WIKI)));

        Assert.assertEquals("page", reference.getName());
        Assert.assertEquals(EntityType.DOCUMENT, reference.getType());
        Assert.assertEquals("space", reference.getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getType());
        Assert.assertEquals("wiki", reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getType());
    }
    
    @org.junit.Test
    public void testResolveWithNoExplicitAndPartialReference()
    {
        try {
            this.resolver.resolve("", EntityType.DOCUMENT);
            Assert.fail("Should have raised an exception");
        } catch (IllegalArgumentException expected) {
            Assert.assertEquals("The resolver parameter doesn't contain an Entity Reference of type [DOCUMENT]",
                expected.getMessage());
        }
    }

    @org.junit.Test
    public void testResolveWithInvalidParameterType()
    {
        try {
            this.resolver.resolve("", EntityType.DOCUMENT, "wrong type");
            Assert.fail("Should have raised an exception");
        } catch (IllegalArgumentException expected) {
            Assert.assertEquals("The resolver parameter doesn't contain an Entity Reference of type [DOCUMENT]",
                expected.getMessage());
        }
    }

    @org.junit.Test
    public void testResolveWithIncompleteExplicitReference()
    {
        try {
            this.resolver.resolve("", EntityType.DOCUMENT, new EntityReference("wiki", EntityType.WIKI));
            Assert.fail("Should have raised an exception");
        } catch (IllegalArgumentException expected) {
            Assert.assertEquals("The resolver parameter doesn't contain an Entity Reference of type [DOCUMENT]",
                expected.getMessage());
        }
    }
}
