/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.api.integrationtest;

import java.util.Arrays;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import org.neo4j.graphdb.Node;
import org.neo4j.kernel.api.DataStatement;
import org.neo4j.kernel.api.properties.Property;

import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PropertyIT extends KernelIntegrationTest
{
    @Test
    public void shouldSetNodePropertyValue() throws Exception
    {
        // GIVEN
        long propertyKeyId;
        long nodeId;
        {
            DataStatement statement = dataStatementInNewTransaction();
            Node node = db.createNode();

            // WHEN
            propertyKeyId = statement.propertyKeyGetOrCreateForName( "clown" );
            nodeId = node.getId();
            statement.nodeSetProperty( nodeId, Property.stringProperty( propertyKeyId, "bozo" ) );

            // THEN
            assertEquals( "bozo", statement.nodeGetProperty( nodeId, propertyKeyId ).value() );

            // WHEN
            commit();
        }
        {
            DataStatement statement = dataStatementInNewTransaction();

            // THEN
            assertEquals( "bozo", statement.nodeGetProperty( nodeId, propertyKeyId ).value() );
        }
    }

    @Test
    public void shouldRemoveSetNodeProperty() throws Exception
    {
        // GIVEN
        long propertyKeyId;
        long nodeId;
        {
            DataStatement statement = dataStatementInNewTransaction();
            Node node = db.createNode();
            propertyKeyId = statement.propertyKeyGetOrCreateForName( "clown" );
            nodeId = node.getId();
            statement.nodeSetProperty( nodeId, Property.stringProperty( propertyKeyId, "bozo" ) );

            // WHEN
            statement.nodeRemoveProperty( nodeId, propertyKeyId );

            // THEN
            assertThat( statement.nodeGetProperty( nodeId, propertyKeyId ), not( isDefinedProperty() ) );

            // WHEN
            commit();
        }

        // THEN
        {
            DataStatement statement = dataStatementInNewTransaction();
            assertThat( statement.nodeGetProperty( nodeId, propertyKeyId ), not( isDefinedProperty() ) );
        }
    }

    @Test
    public void shouldRemoveSetNodePropertyAcrossTransactions() throws Exception
    {
        // GIVEN
        long propertyKeyId;
        long nodeId;
        {
            DataStatement statement = dataStatementInNewTransaction();
            Node node = db.createNode();
            propertyKeyId = statement.propertyKeyGetOrCreateForName( "clown" );
            nodeId = node.getId();
            statement.nodeSetProperty( nodeId, Property.stringProperty( propertyKeyId, "bozo" ) );
            commit();
        }
        {
            DataStatement statement = dataStatementInNewTransaction();

            // WHEN
            Object previous = statement.nodeRemoveProperty( nodeId, propertyKeyId ).value();

            // THEN
            assertEquals( "bozo", previous );
            assertThat( statement.nodeGetProperty( nodeId, propertyKeyId ), not( isDefinedProperty() ) );

            // WHEN
            commit();
        }

        // THEN
        {
            DataStatement statement = dataStatementInNewTransaction();
            assertThat( statement.nodeGetProperty( nodeId, propertyKeyId ), not( isDefinedProperty() ) );
        }
    }

    @Test
    public void shouldSilentlyNotRemoveMissingNodeProperty() throws Exception
    {
        // GIVEN
        long propertyId;
        long nodeId;
        {
            DataStatement statement = dataStatementInNewTransaction();
            Node node = db.createNode();
            propertyId = statement.propertyKeyGetOrCreateForName( "clown" );
            nodeId = node.getId();
            commit();
        }
        {
            DataStatement statement = dataStatementInNewTransaction();

            // WHEN
            Property result = statement.nodeRemoveProperty( nodeId, propertyId );

            // THEN
            assertFalse( "Return no property if removing missing", result.isDefined() );
        }
    }

    @Test
    public void nodeHasPropertyIfSet() throws Exception
    {
        // GIVEN
        long propertyKeyId;
        long nodeId;
        {
            DataStatement statement = dataStatementInNewTransaction();
            Node node = db.createNode();

            // WHEN
            propertyKeyId = statement.propertyKeyGetOrCreateForName( "clown" );
            nodeId = node.getId();
            statement.nodeSetProperty( nodeId, Property.stringProperty( propertyKeyId, "bozo" ) );

            // THEN
            assertThat( statement.nodeGetProperty( nodeId, propertyKeyId ), isDefinedProperty() );

            // WHEN
            commit();
        }
        {
            DataStatement statement = dataStatementInNewTransaction();

            // THEN
            assertThat( statement.nodeGetProperty( nodeId, propertyKeyId ), isDefinedProperty() );
        }
    }

    @Test
    public void nodeHasNotPropertyIfUnset() throws Exception
    {
        long propertyKeyId;
        long nodeId;
        {
            // GIVEN
            DataStatement statement = dataStatementInNewTransaction();
            Node node = db.createNode();

            // WHEN
            propertyKeyId = statement.propertyKeyGetOrCreateForName( "clown" );
            nodeId = node.getId();

            // THEN
            assertThat( statement.nodeGetProperty( nodeId, propertyKeyId ), not( isDefinedProperty() ) );

            // WHEN
            commit();
        }

        {
            DataStatement statement = dataStatementInNewTransaction();

            // THEN
            assertThat( statement.nodeGetProperty( nodeId, propertyKeyId ), not( isDefinedProperty() ) );
        }
    }

    @Test
    public void shouldRollbackSetNodePropertyValue() throws Exception
    {
        // GIVEN
        long propertyKeyId;
        long nodeId;
        {
            DataStatement statement = dataStatementInNewTransaction();
            Node node = db.createNode();
            propertyKeyId = statement.propertyKeyGetOrCreateForName( "clown" );
            nodeId = node.getId();
            commit();
        }

        // WHEN
        {
            DataStatement statement = dataStatementInNewTransaction();
            statement.nodeSetProperty( nodeId, Property.stringProperty( propertyKeyId, "bozo" ) );
            rollback();
        }

        // THEN
        {
            DataStatement statement = dataStatementInNewTransaction();
            assertThat( statement.nodeGetProperty( nodeId, propertyKeyId ), not( isDefinedProperty() ) );
        }
    }

    @Test
    public void shouldUpdateNodePropertyValue() throws Exception
    {
        // GIVEN
        long propertyId;
        long nodeId;
        {
            DataStatement statement = dataStatementInNewTransaction();
            Node node = db.createNode();
            propertyId = statement.propertyKeyGetOrCreateForName( "clown" );
            nodeId = node.getId();
            statement.nodeSetProperty( nodeId, Property.stringProperty( propertyId, "bozo" ) );
            commit();
        }

        // WHEN
        {
            DataStatement statement = dataStatementInNewTransaction();
            statement.nodeSetProperty( nodeId, Property.intProperty( propertyId, 42 ) );
            commit();
        }

        // THEN
        {
            DataStatement statement = dataStatementInNewTransaction();
            assertEquals( 42, statement.nodeGetProperty( nodeId, propertyId ).value() );
        }
    }

    @Test
    public void nodeHasStringPropertyIfSetAndLazyPropertyIfRead() throws Exception
    {
        // GIVEN
        long propertyKeyId;
        long nodeId;
        String value = "Bozo the Clown is a clown character very popular in the United States, peaking in the 1960s";
        {
            DataStatement statement = dataStatementInNewTransaction();
            Node node = db.createNode();

            // WHEN
            propertyKeyId = statement.propertyKeyGetOrCreateForName( "clown" );
            nodeId = node.getId();
            statement.nodeSetProperty( nodeId, Property.stringProperty( propertyKeyId, value ) );

            // THEN
            assertTrue( statement.nodeGetProperty( nodeId, propertyKeyId ).getClass().getSimpleName().equals( "StringProperty" ) );

            // WHEN
            commit();
        }
        {
            DataStatement statement = dataStatementInNewTransaction();

            // THEN
            assertTrue( statement.nodeGetProperty( nodeId, propertyKeyId ).getClass().getSimpleName().equals( "LazyStringProperty" ) );
            assertEquals( value, statement.nodeGetProperty( nodeId, propertyKeyId ).value() );
            assertEquals( value.hashCode(), statement.nodeGetProperty( nodeId, propertyKeyId ).hashCode() );
            assertTrue( statement.nodeGetProperty( nodeId, propertyKeyId ).valueEquals( value ) );
        }
    }

    @Test
    public void nodeHasArrayPropertyIfSetAndLazyPropertyIfRead() throws Exception
    {
        // GIVEN
        long propertyKeyId;
        long nodeId;
        int[] value = new int[] {-1,0,1,2,3,4,5,6,7,8,9,10};
        {
            DataStatement statement = dataStatementInNewTransaction();
            Node node = db.createNode();

            // WHEN
            propertyKeyId = statement.propertyKeyGetOrCreateForName( "numbers" );
            nodeId = node.getId();
            statement.nodeSetProperty( nodeId, Property.intArrayProperty( propertyKeyId, value ) );

            // THEN
            assertTrue( statement.nodeGetProperty( nodeId, propertyKeyId ).getClass().getSimpleName().equals( "IntArrayProperty" ) );

            // WHEN
            commit();
        }
        {
            DataStatement statement = dataStatementInNewTransaction();

            // THEN
            assertTrue( statement.nodeGetProperty( nodeId, propertyKeyId ).getClass().getSimpleName().equals( "LazyArrayProperty" ) );
            assertArrayEquals( value, (int[]) statement.nodeGetProperty( nodeId, propertyKeyId ).value() );
            assertEquals( Arrays.hashCode( value ), statement.nodeGetProperty( nodeId, propertyKeyId ).hashCode() );
            assertTrue( statement.nodeGetProperty( nodeId, propertyKeyId ).valueEquals( value ) );
        }
    }

    private static Matcher<Property> isDefinedProperty()
    {
        return new TypeSafeMatcher<Property>()
        {
            @Override
            protected boolean matchesSafely( Property item )
            {
                return item.isDefined();
            }

            @Override
            public void describeTo( Description description )
            {
                description.appendText( "a defined Property" );
            }
        };
    }
}
