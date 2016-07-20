package com.machinomy.xicity.encoding

case class FailedEncodingError(message: String) extends Error(message)
