package org.javers.core.diff.appenders

import org.javers.core.diff.changetype.map.EntryAddOrRemove
import org.javers.core.diff.changetype.map.EntryAdded
import org.javers.core.diff.changetype.map.EntryRemoved
import org.javers.core.diff.changetype.map.EntryValueChange
import org.javers.core.metamodel.property.Property
import org.javers.core.model.DummyAddress
import org.javers.core.model.DummyUser
import org.javers.core.model.SnapshotEntity
import org.joda.time.LocalDateTime
import spock.lang.Unroll

import static org.javers.core.diff.ChangeAssert.assertThat
import static org.javers.core.model.DummyUser.dummyUser

/**
 * @author bartosz walacik
 */
class MapChangeAppenderTest extends AbstractDiffAppendersTest {

    @Unroll
    def "should not append mapChanges when maps are #what" () {
        given:
        def left =  dummyUser("1").withPrimitiveMap(leftMap)
        def right = dummyUser("1").withPrimitiveMap(rightMap)
        def valueMap = getEntity(DummyUser).getProperty("primitiveMap")

        expect:
        def change = mapChangeAppender().calculateChanges(realNodePair(left,right),valueMap)
        change == null

        where:
        what << ["equal","null"]
        leftMap <<  [["some":1], null]
        rightMap << [["some":1], null]
    }

    def "should set MapChange metadata"() {
        given:
        def left =  dummyUser("1").withPrimitiveMap(null)
        def right = dummyUser("1").withPrimitiveMap(["some":1])
        def primitiveMap = getEntity(DummyUser).getProperty("primitiveMap")

        when:
        def change =  mapChangeAppender().calculateChanges(realNodePair(left,right),primitiveMap)

        then:
        assertThat(change)
                    .hasPropertyName("primitiveMap")
                    .hasInstanceId(DummyUser, "1")
    }

    @Unroll
    def "should append #changeType.simpleName when left map is #leftMap and rightMap is #rightMap"() {
        given:
        def left =  dummyUser("1").withPrimitiveMap(leftMap)
        def right = dummyUser("1").withPrimitiveMap(rightMap)
        Property primitiveMap = getEntity(DummyUser).getProperty("primitiveMap")

        expect:
        def change = mapChangeAppender().calculateChanges(realNodePair(left,right),primitiveMap)
        EntryAddOrRemove entryAddOrRemove = change.entryChanges[0]
        entryAddOrRemove.key == "some"
        entryAddOrRemove.value == 1
        entryAddOrRemove.class == changeType

        where:
        changeType << [EntryAdded,  EntryRemoved,  EntryAdded,           EntryRemoved]
        leftMap <<    [null,        ["some":1],    ["other":1],          ["some":1,"other":1] ]
        rightMap <<   [["some":1],   null,         ["other":1,"some":1], ["other":1] ]
    }

    def "should append EntryValueChanged when Primitive value is changed"() {
        given:
        def left =  dummyUser("1").withPrimitiveMap(["some":1,"other":2] )
        def right = dummyUser("1").withPrimitiveMap(["some":2,"other":2])
        Property primitiveMap = getEntity(DummyUser).getProperty("primitiveMap")

        when:
        def change =  mapChangeAppender().calculateChanges(realNodePair(left,right),primitiveMap)

        then:
        EntryValueChange entryValueChanged = change.entryChanges[0]
        entryValueChanged.key == "some"
        entryValueChanged.leftValue == 1
        entryValueChanged.rightValue == 2
    }

    def "should append EntryValueChanged when ValueType value is changed"() {

        def dayOne = new LocalDateTime(2000,1,1,12,1)
        def dayTwo = new LocalDateTime(2000,1,1,12,2)

        given:
        def left =  dummyUser("1").withValueMap(["some":dayOne, "other":dayTwo] )
        def right = dummyUser("1").withValueMap(["some":dayTwo, "other":dayTwo])
        Property valueMap = getEntity(DummyUser).getProperty("valueMap")

        when:
        def change = mapChangeAppender().calculateChanges(realNodePair(left,right),valueMap)

        then:
        EntryValueChange entryValueChanged = change.entryChanges[0]
        entryValueChanged.key == "some"
        entryValueChanged.leftValue ==   dayOne
        entryValueChanged.rightValue ==  dayTwo
    }

    @Unroll
    def "should detect value changes in map of ValueObjects "() {
        def javers = org.javers.core.JaversBuilder.javers().build()
        given:
        def left = new SnapshotEntity(mapPrimitiveToVO: leftMap)
        def right = new SnapshotEntity(mapPrimitiveToVO: rightMap)

        when:
        def diff = javers.compare(left, right)

        then:
        println diff
        diff.changes.size() == extpectedChanges

        where:
        leftMap << [["New York" : new DummyAddress(city: "New York", street: "Maple St")],
                    ["New York" : new DummyAddress(city: "New York", street: "Maple St"),"Alabama": new DummyAddress(city: "Buffalo", street: "Maple St")],
                    [:]]
        rightMap << [["New York":new DummyAddress(city: "New York", street: "Troy Ave")],
                     ["New York":new DummyAddress(city: "New York", street: "Maple St"),"Toronto":  new DummyAddress(city: "Buffalo", street: "Maple St")],
                     ["New York":new DummyAddress(city: "New York", street: "Maple St")]]
        extpectedChanges << [2, 1, 1]
    }
}
