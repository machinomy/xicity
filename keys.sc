import com.machinomy.xicity.Hex
import com.machinomy.xicity.sodium._

val a = Sodium.newKeypair.get
val b = Sodium.newKeypair.get

val ck = Sodium.precomputeCombinedKey(a.secretKey, b.publicKey).get
val nonce = Sodium.newNonce
val text = PlainText("foo".getBytes)

val cipher = Box.encrypt(ck, nonce, text).get
Hex.encode(cipher.bytes)
val decrypted = Box.decrypt(ck, nonce, cipher).get
Hex.decode(Hex.encode(decrypted.bytes)).map(_.toChar).mkString
