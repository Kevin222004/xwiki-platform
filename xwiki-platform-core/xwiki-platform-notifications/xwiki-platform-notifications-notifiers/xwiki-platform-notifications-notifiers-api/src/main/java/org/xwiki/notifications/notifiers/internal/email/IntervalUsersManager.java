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
package org.xwiki.notifications.notifiers.internal.email;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.preferences.NotificationEmailInterval;
import org.xwiki.notifications.preferences.email.NotificationEmailUserPreferenceManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Gather and cache users depending on each {@link NotificationEmailInterval}.
 * 
 * @version $Id$
 * @since 11.10.6
 * @since 12.6
 */
@Component(roles = IntervalUsersManager.class)
@Singleton
public class IntervalUsersManager
{
    private static final int BATCH_SIZE = 100;

    private static class WikiEntry
    {
        private final Map<NotificationEmailInterval, List<DocumentReference>> usersPerInterval =
            new ConcurrentHashMap<>();

        private final Map<DocumentReference, NotificationEmailInterval> intervalPerUser =
            new ConcurrentHashMap<>();
    }

    /**
     * The query to perform to get all users having a not-empty email address. Here, we re using
     * <code>length(objUser.email) > 0</code> instead of <code>objUser.email <> ''</code> because ORACLE stores NULL
     * instead of empty strings. But if we do <code>objUser.email <> NULL AND objUser.email <> ''</code>, then we have
     * wrong results with MySQL. This <code>length()</code> trick allows us to use the same query on every database we
     * support, but a better solution would be to write a different query for ORACLE than for the others, because this
     * length() may be bad for performances.
     */
    // TODO: try with the solution suggested by Sergiu Dumitriu there: https://jira.xwiki.org/browse/XWIKI-14914
    private static final String XWQL_QUERY = "select distinct doc.fullName from Document doc, "
        + "doc.object(XWiki.XWikiUsers) objUser where objUser.active = 1 and length(objUser.email) > 0 "
        + "order by doc.fullName";

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Inject
    private EntityReferenceFactory referenceFactory;

    private final Map<String, WikiEntry> usersCache = new ConcurrentHashMap<>();

    @Inject
    private NotificationEmailUserPreferenceManager emailUserPreferenceManager;

    /**
     * Get all users in a wiki which are configured with the passed interval.
     * 
     * @param interval the interval
     * @param wiki the wiki identifier
     * @return the references of the users
     * @throws QueryException if the gathering of users failed
     */
    public List<DocumentReference> getUsers(NotificationEmailInterval interval, String wiki) throws QueryException
    {
        try {
            return getWikiEntry(wiki).usersPerInterval.computeIfAbsent(interval, k -> loadUsers(k, wiki));
        } catch (RuntimeException e) {
            throw (QueryException) e.getCause();
        }
    }

    private WikiEntry getWikiEntry(String wiki)
    {
        return this.usersCache.computeIfAbsent(wiki, k -> new WikiEntry());
    }

    /**
     * @param userDocumentReference the reference of the user
     * @return the interval of the user
     */
    public NotificationEmailInterval getInterval(DocumentReference userDocumentReference)
    {
        return getWikiEntry(userDocumentReference.getWikiReference().getName()).intervalPerUser
            .computeIfAbsent(this.referenceFactory.getReference(userDocumentReference), this::loadInterval);
    }

    private NotificationEmailInterval loadInterval(DocumentReference userDocumentReference)
    {
        UserReference userReference = userReferenceResolver.resolve(userDocumentReference);
        return emailUserPreferenceManager.getInterval(userReference);
    }

    private List<DocumentReference> loadUsers(NotificationEmailInterval targetInterval, String wiki)
    {
        List<DocumentReference> userReferences = new ArrayList<>();

        int batchSize = BATCH_SIZE;
        for (int offset = 0; batchSize == BATCH_SIZE; offset += BATCH_SIZE) {
            try {
                batchSize = loadUsers(targetInterval, wiki, offset, userReferences);
            } catch (QueryException e) {
                throw new RuntimeException(e);
            }
        }

        return userReferences;
    }

    private int loadUsers(NotificationEmailInterval targetInterval, String wiki, int offset,
        List<DocumentReference> userReferences) throws QueryException
    {
        Query query = this.queryManager.createQuery(XWQL_QUERY, Query.XWQL);
        query.setWiki(wiki);
        query.setLimit(BATCH_SIZE);
        query.setOffset(offset);

        List<String> users = (List) query.execute();

        if (!users.isEmpty()) {
            WikiReference wikiReference = new WikiReference(wiki);

            for (String user : users) {
                DocumentReference userDocumentReference = this.referenceResolver.resolve(user, wikiReference);
                NotificationEmailInterval userInterval = getInterval(userDocumentReference);
                if (userInterval == targetInterval) {
                    // Avoid duplicated strings as much as possible in the cache
                    userReferences.add(this.referenceFactory.getReference(userDocumentReference));
                }
            }
        }

        return users.size();
    }

    /**
     * @param wikiId the id of the wiki to invalidate
     */
    public void invalidateWiki(String wikiId)
    {
        this.usersCache.remove(wikiId);
    }

    /**
     * @param userReference the reference of the user to invalidate
     */
    public void invalidateUser(DocumentReference userReference)
    {
        WikiEntry wikiEntry = this.usersCache.get(userReference.getWikiReference().getName());

        if (wikiEntry != null) {
            wikiEntry.intervalPerUser.remove(userReference);
            wikiEntry.usersPerInterval.clear();
        }
    }
}
