package io.machinomy.xicity.test

import org.scalatest.FunSuite
import org.scalatest.concurrent.Eventually

class Stub extends FunSuite with Eventually {

  test("1 is 1") {
    assert(1 == 1)
  }

}
