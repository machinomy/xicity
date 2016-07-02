package com.machinomy.xicity.connectivity

import java.net.InetSocketAddress

import akka.actor.ActorRef

trait ListenerBehavior {
  def didBind(wire: ActorRef): ListenerBehavior
  def didConnect(remoteAddress: InetSocketAddress): ListenerBehavior
  def didClose(): ListenerBehavior
  def didDisconnect(): ListenerBehavior
}
