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
 *
 */

package com.xpn.xwiki.plugin.ldap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.novell.ldap.LDAPConnection;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheNeedsRefreshException;

/**
 * LDAP communication tool.
 * 
 * @version $Id$
 * @since 1.3 M2
 */
public class XWikiLDAPUtils
{
    /**
     * Logging tool.
     */
    private static final Log LOG = LogFactory.getLog(XWikiLDAPUtils.class);

    /**
     * LDAP objectClass parameter.
     */
    private static final String LDAP_OBJECTCLASS = "objectClass";

    /**
     * The name of the LDAP groups cache.
     */
    private static final String CACHE_NAME_GROUPS = "groups";

    /**
     * Default unique user field name.
     */
    private static final String LDAP_DEFAULT_UID = "cn";

    /**
     * Contains caches for each LDAP host:port.
     */
    private static Map<String, Map<String, XWikiCache>> cachePool =
        new HashMap<String, Map<String, XWikiCache>>();

    /**
     * The LDAP connection.
     */
    private XWikiLDAPConnection connection;

    /**
     * The LDAP attribute containing the identifier for a user.
     */
    private String uidAttributeName = LDAP_DEFAULT_UID;

    /**
     * Different LDAP implementations groups classes names.
     */
    private Collection<String> groupClasses = XWikiLDAPConfig.DEFAULT_GROUP_CLASSES;

    /**
     * Different LDAP implementations groups member property name.
     */
    private Collection<String> groupMemberFields = XWikiLDAPConfig.DEFAULT_GROUP_MEMBERFIELDS;

    /**
     * Create an instance of {@link XWikiLDAPUtils}.
     * 
     * @param connection the XWiki LDAP connection tool.
     */
    public XWikiLDAPUtils(XWikiLDAPConnection connection)
    {
        this.connection = connection;
    }

    /**
     * @param uidAttributeName the LDAP attribute containing the identifier for a user.
     */
    public void setUidAttributeName(String uidAttributeName)
    {
        this.uidAttributeName = uidAttributeName;
    }

    /**
     * @return the LDAP attribute containing the identifier for a user.
     */
    public String getUidAttributeName()
    {
        return uidAttributeName;
    }

    /**
     * @param groupClasses the different LDAP implementations groups classes names.
     */
    public void setGroupClasses(Collection<String> groupClasses)
    {
        this.groupClasses = groupClasses;
    }

    /**
     * @return the different LDAP implementations groups classes names.
     */
    public Collection<String> getGroupClasses()
    {
        return groupClasses;
    }

    /**
     * @param groupMemberFields the different LDAP implementations groups member property name.
     */
    public void setGroupMemberFields(Collection<String> groupMemberFields)
    {
        this.groupMemberFields = groupMemberFields;
    }

    /**
     * @return the different LDAP implementations groups member property name.
     */
    public Collection<String> getGroupMemberFields()
    {
        return groupMemberFields;
    }

    /**
     * Get the cache with the provided name for a particular LDAP server.
     * 
     * @param cacheName the name of the cache.
     * @param context the XWiki context.
     * @return the cache.
     * @throws XWikiException error when creating the cache.
     */
    public XWikiCache getCache(String cacheName, XWikiContext context) throws XWikiException
    {
        XWikiCache cache;

        String cacheKey =
            getUidAttributeName() + "." + connection.getConnection().getHost() + ":"
                + connection.getConnection().getPort();

        Map<String, XWikiCache> cacheMap;

        if (cachePool.containsKey(cacheKey)) {
            cacheMap = cachePool.get(cacheKey);
        } else {
            cacheMap = new HashMap<String, XWikiCache>();
            cachePool.put(cacheKey, cacheMap);
        }

        if (cacheMap.containsKey(cacheName)) {
            cache = (XWikiCache) cacheMap.get(cacheName);
        } else {
            cache = context.getWiki().getCacheService().newCache("ldap." + cacheName);
            cacheMap.put(cacheName, cache);
        }

        return cache;
    }

    /**
     * @return get {@link XWikiLDAPConnection}.
     */
    public XWikiLDAPConnection getConnection()
    {
        return connection;
    }

    /**
     * Execute LDAP query to get all group's members.
     * 
     * @param groupDN the group to retrieve the members of and scan for subgroups.
     * @return the LDAP search result.
     */
    private List<XWikiLDAPSearchAttribute> searchGroupsMembers(String groupDN)
    {
        String[] attrs = new String[2 + getGroupMemberFields().size()];

        int i = 0;
        attrs[i++] = LDAP_OBJECTCLASS;
        attrs[i++] = getUidAttributeName();
        for (String groupMember : getGroupMemberFields()) {
            attrs[i++] = groupMember;
        }

        return getConnection().searchLDAP(groupDN, null, attrs, LDAPConnection.SCOPE_BASE);
    }

    /**
     * Extract group's members from provided LDAP search result.
     * 
     * @param searchAttributeList the LDAP search result.
     * @param memberMap the result: maps DN to member id.
     * @param subgroups return all the subgroups identified.
     * @param context the XWiki context.
     */
    private void getGroupMembers(List<XWikiLDAPSearchAttribute> searchAttributeList,
        Map<String, String> memberMap, List<String> subgroups, XWikiContext context)
    {
        for (XWikiLDAPSearchAttribute searchAttribute : searchAttributeList) {
            String key = searchAttribute.name;
            if (getGroupMemberFields().contains(key.toLowerCase())) {

                // or subgroup
                String member = searchAttribute.value;

                // we check for subgroups recursive call to scan all subgroups and identify members
                // and their uid
                getGroupMembers(member, memberMap, subgroups, context);
            }
        }
    }

    /**
     * Get all members of a given group based on the groupDN. If the group contains subgroups get
     * these members as well. Retrieve an identifier for each member.
     * 
     * @param groupDN the group to retrieve the members of and scan for subgroups.
     * @param memberMap the result: maps DN to member id.
     * @param subgroups all the subgroups identified.
     * @param searchAttributeList the groups members found in LDAP search.
     * @param context the XWiki context.
     * @return whether the groupDN is actually a group.
     */
    public boolean getGroupMembers(String groupDN, Map<String, String> memberMap,
        List<String> subgroups, List<XWikiLDAPSearchAttribute> searchAttributeList,
        XWikiContext context)
    {
        boolean isGroup = false;

        String id = null;

        for (XWikiLDAPSearchAttribute searchAttribute : searchAttributeList) {
            String key = searchAttribute.name;

            if (key.equalsIgnoreCase(LDAP_OBJECTCLASS)) {
                String objectName = searchAttribute.value;
                if (getGroupClasses().contains(objectName.toLowerCase())) {
                    isGroup = true;
                }
            } else if (key.equalsIgnoreCase(getUidAttributeName())) {
                id = searchAttribute.value;
            }
        }

        if (!isGroup) {
            if (id == null) {
                LOG.error("Could not find attribute " + getUidAttributeName() + " for LDAP dn "
                    + groupDN);
            }

            if (!memberMap.containsKey(groupDN)) {
                memberMap.put(groupDN, id == null ? "" : id);
            }
        } else {
            // remember this group
            if (subgroups != null) {
                subgroups.add(groupDN);
            }

            getGroupMembers(searchAttributeList, memberMap, subgroups, context);
        }

        return isGroup;
    }

    /**
     * Get all members of a given group based on the groupDN. If the group contains subgroups get
     * these members as well. Retrieve an identifier for each member.
     * 
     * @param groupDN the group to retrieve the members of and scan for subgroups.
     * @param memberMap the result: maps DN to member id.
     * @param subgroups all the subgroups identified.
     * @param context the XWiki context.
     * @return whether the groupDN is actually a group.
     */
    public boolean getGroupMembers(String groupDN, Map<String, String> memberMap,
        List<String> subgroups, XWikiContext context)
    {
        boolean isGroup = false;

        // break out if there is a look of groups
        if (subgroups != null && subgroups.contains(groupDN)) {
            return true;
        }

        List<XWikiLDAPSearchAttribute> searchAttributeList = searchGroupsMembers(groupDN);

        if (searchAttributeList != null) {
            isGroup =
                getGroupMembers(groupDN, memberMap, subgroups, searchAttributeList, context);
        }

        return isGroup;
    }

    /**
     * Get group members from cache or update it from LDAP if it is not already cached.
     * 
     * @param groupDN the name of the group.
     * @param context the XWiki context.
     * @return the members of the group.
     * @throws XWikiException error when getting the group cache.
     */
    public Map<String, String> getGroupMembers(String groupDN, XWikiContext context)
        throws XWikiException
    {
        Map<String, String> groupMembers = null;

        XWikiLDAPConfig config = XWikiLDAPConfig.getInstance();

        XWikiCache cache = getCache(CACHE_NAME_GROUPS, context);

        synchronized (cache) {
            try {
                groupMembers =
                    (Map<String, String>) cache.getFromCache(groupDN, config
                        .getCacheExpiration(context));
            } catch (XWikiCacheNeedsRefreshException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Cache does not caontains group " + groupDN, e);
                }
            }
        }

        if (groupMembers == null) {
            Map<String, String> members = new HashMap<String, String>();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Retrieving Members of the group: " + groupDN);
            }

            boolean isGroup = getGroupMembers(groupDN, members, new ArrayList<String>(), context);

            if (isGroup) {
                groupMembers = members;
                synchronized (cache) {
                    cache.putInCache(groupDN, groupMembers);
                }
            }
        }

        return groupMembers;
    }

    /**
     * Locates the user in the Map: either the user is a value or the key starts with the LDAP
     * syntax.
     * 
     * @param userName the name of the user.
     * @param groupMembers the members of LDAP group.
     * @param context the XWiki context.
     * @return the full user name.
     */
    protected String findInGroup(String userName, Map<String, String> groupMembers,
        XWikiContext context)
    {
        String result = null;

        String ldapuser = getUidAttributeName() + "=" + userName.toLowerCase();

        for (Map.Entry<String, String> entry : groupMembers.entrySet()) {
            // implementing it case-insensitive for now
            if (userName.equalsIgnoreCase(entry.getValue())
                || entry.getKey().startsWith(ldapuser)) {
                return entry.getKey();
            }
        }

        return result;
    }

    /**
     * Check if user is in provided LDAP group.
     * 
     * @param userName the user name.
     * @param groupDN the LDAP group DN.
     * @param context the XWiki context.
     * @return user's DB if the user is in the LDAP group, null otherwise.
     * @throws XWikiException error when getting the group cache.
     */
    public String isUserInGroup(String userName, String groupDN, XWikiContext context)
        throws XWikiException
    {
        String userDN = null;

        if (groupDN.length() > 0) {
            Map<String, String> groupMembers = getGroupMembers(groupDN, context);

            // check if user is in the list
            userDN = findInGroup(userName, groupMembers, context);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Found user dn in user group:" + userDN);
            }

            // if a usergroup is specified THEN the user MUST be in the group to validate in
            // LDAP
            if (userDN == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("LDAP authentication failed: user not in LDAP user group");
                }

                // no valid LDAP user from the group
                return null;
            }
        }

        return userDN;
    }
}
