package com.lulema.app.backup

import com.lulema.app.data.*
import kotlinx.serialization.Serializable
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@Serializable
data class Backup(val formatVersion: Int = 1, val exportedAt: Long = System.currentTimeMillis(), val records: List<Entry>, val settings: Preferences) {
 fun validate() {
  require(formatVersion==1) { "不支持此备份版本" }
  require(exportedAt > 0 && records.size <= 50000) { "备份大小或时间无效" }
  require(records.map { it.id }.distinct().size==records.size) { "备份包含重复 ID" }
  records.forEach { it.validate() }; settings.validate()
 }
}
@Serializable
private data class Encrypted(val encryptedVersion:Int=1,val salt:String,val iv:String,val ciphertext:String)
object BackupCodec {
 const val MAX_BYTES=16*1024*1024
 private fun checkDepth(text:String) {
  var depth=0;var quoted=false;var escaped=false
  text.forEach { c ->
   if(quoted) { if(escaped)escaped=false else if(c=='\\')escaped=true else if(c=='"')quoted=false }
   else when(c) { '"'->quoted=true; '{','['->{depth++;require(depth<=24) {"备份嵌套过深"}}; '}',']'->{depth--;require(depth>=0) {"备份结构无效"}} }
  }
  require(!quoted && depth==0) {"备份结构无效"}
 }
 private val b64=Base64.getEncoder()
 private fun key(password:CharArray,salt:ByteArray): SecretKeySpec {
  val spec=PBEKeySpec(password,salt,210000,256)
  return try { SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,"AES") } finally { spec.clearPassword() }
 }
 fun encode(backup:Backup,password:CharArray?=null): ByteArray {
  backup.validate()
  val plain=ArchiveJson.encodeToString(backup).toByteArray(Charsets.UTF_8)
  require(plain.size <= MAX_BYTES/2) { "数据过大，请分批整理档案" }
  if(password==null) return plain
  require(password.size>=8) { "备份密码至少 8 位" }
  val salt=ByteArray(16).also { SecureRandom().nextBytes(it) }
  val iv=ByteArray(12).also { SecureRandom().nextBytes(it) }
  val cipher=Cipher.getInstance("AES/GCM/NoPadding")
  cipher.init(Cipher.ENCRYPT_MODE,key(password,salt),GCMParameterSpec(128,iv))
  cipher.updateAAD("LuLeMa/backup/1".toByteArray())
  return ArchiveJson.encodeToString(Encrypted(salt=b64.encodeToString(salt),iv=b64.encodeToString(iv),ciphertext=b64.encodeToString(cipher.doFinal(plain)))).toByteArray(Charsets.UTF_8)
 }
 fun decode(bytes:ByteArray,password:CharArray?=null):Backup {
  require(bytes.size in 1..MAX_BYTES) { "备份文件为空或超过 16 MiB" }
  val text=bytes.toString(Charsets.UTF_8)
  checkDepth(text)
  val envelope=try {ArchiveJson.decodeFromString<Encrypted>(text)} catch(_:kotlinx.serialization.SerializationException) {null}
  val plain=if(envelope!=null) {
   require(password!=null && password.size>=8) { "请输入至少 8 位的备份密码" }
   val e=envelope
   require(e.encryptedVersion==1) { "不支持此加密版本" }
   val salt=Base64.getDecoder().decode(e.salt);val iv=Base64.getDecoder().decode(e.iv)
   require(salt.size==16 && iv.size==12) { "加密参数无效" }
   val cipher=Cipher.getInstance("AES/GCM/NoPadding")
   cipher.init(Cipher.DECRYPT_MODE,key(password,salt),GCMParameterSpec(128,iv))
   cipher.updateAAD("LuLeMa/backup/1".toByteArray())
   cipher.doFinal(Base64.getDecoder().decode(e.ciphertext)).toString(Charsets.UTF_8)
  } else text
  checkDepth(plain)
  return ArchiveJson.decodeFromString<Backup>(plain).also { it.validate() }
 }
}
