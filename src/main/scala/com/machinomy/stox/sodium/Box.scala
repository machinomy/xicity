package com.machinomy.stox.sodium

object Box {
  /** The encryption function takes a Combined Key, a Nonce, and a Plain Text, and
    * returns a Cipher Text.  It uses \texttt{crypto_box_afternm} to perform the
    * encryption.  The meaning of the sentence "encrypting with a secret key, a
    * public key, and a nonce" is: compute a combined key from the secret key and the
    * public key and then use the encryption function for the transformation.
    */
  def encrypt(ck: CombinedKey, nonce: Nonce, text: PlainText): Option[CipherText] = Sodium.boxAfterNM(ck, nonce, text)

  /** The decryption function takes a Combined Key, a Nonce, and a Cipher Text, and
    * returns either a Plain Text or an error.  It uses
    * \texttt{crypto_box_open_afternm} from the NaCl library.  Since the cipher is
    * symmetric, the encryption function can also perform decryption, but will not
    * perform message authentication, so the implementation must be careful to use
    * the correct functions.
    */
  def decrypt(ck: CombinedKey, nonce: Nonce, cipherText: CipherText): Option[PlainText] = Sodium.boxOpenAfterNM(ck, nonce, cipherText)
}
