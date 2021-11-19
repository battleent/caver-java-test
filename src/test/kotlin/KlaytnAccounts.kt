import CaverHelper.caver
import com.klaytn.caver.wallet.keyring.SingleKeyring

object KlaytnAccounts {
    val feePayer: SingleKeyring = caver.wallet.keyring.create(
        "0xa9b8617cbbbb2f61b5d6533141aaa78a4a723750",
        "0x9736c2f3951d10d706a890ce2b3c6f5c9220ec847a7d8cf2cf8b47352239a323"
    )
    val from: SingleKeyring = caver.wallet.keyring.create(
        "0x02b7ee2ab67cdfe5ce703d54f3a78ae91ef2a195",
        "0xe723082de00fcbc53a97d35e810b791db56f123a2528737bf21df441dc24c966"
    )

    val to: SingleKeyring = caver.wallet.keyring.create(
        "0x4f188ec02553f532f78bd1e47472ca30354ecec9",
        "0x78557dc9dfd3306d1a03fb1d73fc96a5844a91691b58d0cddfedb5079a5991f4"
    )

    val devAdmin = caver.wallet.keyring.create(
        "0x4f188ec02553f532f78bd1e47472ca30354ecec9",
        "0x78557dc9dfd3306d1a03fb1d73fc96a5844a91691b58d0cddfedb5079a5991f4"
    )
}
