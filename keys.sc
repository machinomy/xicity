import com.machinomy.xicity.Hex
import com.machinomy.xicity.sodium._

val alice = Sodium.newKeypair.get
val bob = Sodium.newKeypair.get

val ck = Sodium.precomputeCombinedKey(alice.secretKey, bob.publicKey).get
val nonce = Sodium.newNonce
val text = PlainText("Hello, World!".getBytes)



val cipher = Box.encrypt(ck, nonce, text).get
Hex.encode(cipher.bytes)
val decrypted = Box.decrypt(ck, nonce, cipher).get
Hex.decode(Hex.encode(decrypted.bytes)).map(_.toChar).mkString
