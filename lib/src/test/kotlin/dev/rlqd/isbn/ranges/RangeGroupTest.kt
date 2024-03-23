package dev.rlqd.isbn.ranges

import kotlin.test.Test
import kotlin.test.assertEquals

class RangeGroupTest {
    @Test
    fun findGS1Range() {
        val ranges = RangeGroup(
            "International ISBN Agency",
            listOf(
                Range(0,5999999,1),
                Range(6000000,6499999,3),
                Range(6500000,6599999,2),
                Range(6600000,6999999,0),
                Range(7000000,7999999,1),
                Range(8000000,9499999,2),
                Range(9500000,9899999,3),
                Range(9900000,9989999,4),
                Range(9990000,9999999,5),
            ),
        )
        assertEquals(1, ranges.findRange(5040978)?.length)
        assertEquals(5, ranges.findRange(9991234)?.length)
    }

    @Test
    fun findGroupRange() {
        val ranges = RangeGroup(
            "former U.S.S.R",
            listOf(
                Range(0, 49999, 5),
                Range(50000, 99999, 4),
                Range(100000, 1999999, 2),
                Range(2000000, 3619999, 3),
                Range(3620000, 3623999, 4),
                Range(3624000, 3629999, 5),
                Range(3630000, 4209999, 3),
                Range(4210000, 4299999, 4),
                Range(4300000, 4309999, 3),
                Range(4310000, 4399999, 4),
                Range(4400000, 4409999, 3),
                Range(4410000, 4499999, 4),
                Range(4500000, 6039999, 3),
                Range(6040000, 6049999, 7),
                Range(6050000, 6999999, 3),
                Range(7000000, 8499999, 4),
                Range(8500000, 8999999, 5),
                Range(9000000, 9099999, 6),
                Range(9100000, 9199999, 5),
                Range(9200000, 9299999, 4),
                Range(9300000, 9499999, 5),
                Range(9500000, 9500999, 7),
                Range(9501000, 9799999, 4),
                Range(9800000, 9899999, 5),
                Range(9900000, 9909999, 7),
                Range(9910000, 9999999, 4)
            ),
        )
        assertEquals(2, ranges.findRange(409787)?.length) //0409787
        assertEquals(4, ranges.findRange(9211234)?.length)
    }
}