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
package org.xwiki.mail.script;

import java.util.UUID;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ScriptMimeMessage}.
 *
 * @version $Id$
 * @since 7.1M2
 */
public class ScriptMimeMessageTest
{
    private ScriptMimeMessage scriptMessage;

    @Before
    public void setUp() throws Exception
    {
        Execution execution = mock(Execution.class);
        ComponentManager componentManager = mock(ComponentManager.class);

        this.scriptMessage = new ScriptMimeMessage(execution, componentManager);
    }

    @Test
    public void setSubject() throws Exception
    {
        this.scriptMessage.setSubject("lorem ipsum");
        assertEquals("lorem ipsum", this.scriptMessage.getSubject());
    }

    @Test
    public void setFrom() throws Exception
    {
        this.scriptMessage.setFrom(InternetAddress.parse("john@doe.com")[0]);
        assertArrayEquals(InternetAddress.parse("john@doe.com"), this.scriptMessage.getFrom());
    }

    @Test
    public void addRecipients() throws Exception
    {
        Address[] address = InternetAddress.parse("john@doe.com,jane@doe.com,jannie@doe.com");
        this.scriptMessage.addRecipients(Message.RecipientType.TO, address);
        assertArrayEquals(address, this.scriptMessage.getRecipients(Message.RecipientType.TO));
    }

    @Test
    public void addRecipient() throws Exception
    {
        this.scriptMessage.addRecipient(Message.RecipientType.TO, InternetAddress.parse("john@doe.com")[0]);
        assertArrayEquals(InternetAddress.parse("john@doe.com"),
            this.scriptMessage.getRecipients(Message.RecipientType.TO));
    }

    @Test
    public void addHeader() throws Exception
    {
        String batchId = UUID.randomUUID().toString();
        String mailId = UUID.randomUUID().toString();

        this.scriptMessage.addHeader("X-BatchID", batchId);
        this.scriptMessage.addHeader("X-MailID", mailId);

        assertEquals(batchId, this.scriptMessage.getHeader("X-BatchID", null));
        assertEquals(mailId, this.scriptMessage.getHeader("X-MailID", null));
    }
}
