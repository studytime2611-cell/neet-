package com.example

import com.example.util.DateUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun suggestMissionFocusMinutes_parsesDurationsAccurately() {
    assertEquals(45, DateUtils.suggestMissionFocusMinutes("Kinematics 45m"))
    assertEquals(90, DateUtils.suggestMissionFocusMinutes("Electrostatics 1.5h PYQ"))
    assertEquals(60, DateUtils.suggestMissionFocusMinutes("NCERT Cell Revision 60 mins"))
    assertNull(DateUtils.suggestMissionFocusMinutes("Rotational Motion 40 PYQ"))
  }
}
