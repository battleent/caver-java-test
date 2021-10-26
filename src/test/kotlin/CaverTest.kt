import CaverHelper.caver
import CaverHelper.chargeKlay
import CaverHelper.deploy
import CaverHelper.execution
import CaverHelper.transfer
import CaverHelper.updateAccount
import com.klaytn.caver.contract.SendOptions
import org.junit.jupiter.api.*
import org.web3j.protocol.exceptions.TransactionException
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CaverTest {

    @BeforeAll
    fun init() {
        chargeKlay(KlaytnAccounts.feePayer.address)
    }

    @Test
    fun `클레이 조회 테스트`() {
        val result = caver.rpc.klay.getBalance(KlaytnAccounts.from.address).send()
        assert(result.value > BigInteger.ZERO)
    }

    @Test
    fun `클레이 전송 테스트`() {
        val value = BigInteger.valueOf(1)
        val receiptData = transfer(KlaytnAccounts.feePayer, KlaytnAccounts.from.address, value, false)

        assert(Integer.decode(receiptData.value).toBigInteger() == value)
    }

    @Test
    fun `클레이 대납 전송 테스트`() {
        val value = BigInteger.valueOf(1)
        val receiptData = transfer(KlaytnAccounts.feePayer, KlaytnAccounts.from.address, value)

        assert(Integer.decode(receiptData.value).toBigInteger() == value)
    }

    @Test
    fun `계정 권한 업데이트 테스트`() {
        val keyring = caver.wallet.keyring.generate()
        val roleTransactionKeyring = caver.wallet.keyring.generate()
        val roleAccountUpdateKeyring = caver.wallet.keyring.generate()
        val roleFeePayerKeyring = caver.wallet.keyring.generate()
        val newKeyring = caver.wallet.keyring.create(
            keyring.address,
            listOf(
                listOf(roleTransactionKeyring.key.privateKey),
                listOf(roleAccountUpdateKeyring.key.privateKey),
                listOf(roleFeePayerKeyring.key.privateKey)
            ).map { it.toTypedArray() }
        )
        // 계정 업데이트
        assertDoesNotThrow {
            val keyring = caver.wallet.keyring.create(keyring.address, keyring.key.privateKey)
            updateAccount(keyring, newKeyring)
        }
        // 계정 업데이트는 RoleAccountUpdate 역활의 키로만 서명할 수 있다.
        assertThrows<TransactionException> {
            val keyring = caver.wallet.keyring.create(keyring.address, keyring.key.privateKey)
            updateAccount(keyring, newKeyring)
        }
        assertDoesNotThrow {
            val keyring = caver.wallet.keyring.create(keyring.address, roleAccountUpdateKeyring.key.privateKey)
            updateAccount(keyring, newKeyring)
        }

        chargeKlay(keyring.address)

        // RoleAccountUpdate 이외의 트랜잭션은 RoleTransaction 역활의 키로만 서명할 수 있다.
        assertThrows<TransactionException> {
            val keyring = caver.wallet.keyring.create(keyring.address, keyring.key.privateKey)
            val value = caver.rpc.klay.getBalance(keyring.address).send().value
            transfer(keyring, KlaytnAccounts.feePayer.address, value)
        }
        assertThrows<TransactionException> {
            val keyring = caver.wallet.keyring.create(keyring.address, roleFeePayerKeyring.key.privateKey)
            val value = caver.rpc.klay.getBalance(keyring.address).send().value
            transfer(keyring, KlaytnAccounts.feePayer.address, value)
        }
        assertDoesNotThrow {
            val keyring = caver.wallet.keyring.create(keyring.address, roleTransactionKeyring.key.privateKey)
            val value = caver.rpc.klay.getBalance(keyring.address).send().value
            transfer(keyring, KlaytnAccounts.feePayer.address, value)
        }
    }

    @Test
    fun `컨트랙트 테스트`() {
        val byteCode =
            "608060405234801561001057600080fd5b5061051f806100206000396000f3fe608060405234801561001057600080fd5b50600436106100365760003560e01c8063693ec85e1461003b578063e942b5161461016f575b600080fd5b6100f46004803603602081101561005157600080fd5b810190808035906020019064010000000081111561006e57600080fd5b82018360208201111561008057600080fd5b803590602001918460018302840111640100000000831117156100a257600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192905050506102c1565b6040518080602001828103825283818151815260200191508051906020019080838360005b83811015610134578082015181840152602081019050610119565b50505050905090810190601f1680156101615780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b6102bf6004803603604081101561018557600080fd5b81019080803590602001906401000000008111156101a257600080fd5b8201836020820111156101b457600080fd5b803590602001918460018302840111640100000000831117156101d657600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019064010000000081111561023957600080fd5b82018360208201111561024b57600080fd5b8035906020019184600183028401116401000000008311171561026d57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192905050506103cc565b005b60606000826040518082805190602001908083835b602083106102f957805182526020820191506020810190506020830392506102d6565b6001836020036101000a03801982511681845116808217855250505050505090500191505090815260200160405180910390208054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156103c05780601f10610395576101008083540402835291602001916103c0565b820191906000526020600020905b8154815290600101906020018083116103a357829003601f168201915b50505050509050919050565b806000836040518082805190602001908083835b6020831061040357805182526020820191506020810190506020830392506103e0565b6001836020036101000a0380198251168184511680821785525050505050509050019150509081526020016040518091039020908051906020019061044992919061044e565b505050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061048f57805160ff19168380011785556104bd565b828001600101855582156104bd579182015b828111156104bc5782518255916020019190600101906104a1565b5b5090506104ca91906104ce565b5090565b6104f091905b808211156104ec5760008160009055506001016104d4565b5090565b9056fea165627a7a723058203ffebc792829e0434ecc495da1b53d24399cd7fff506a4fd03589861843e14990029"
        val abi =
            "[{\"constant\":true,\"inputs\":[{\"name\":\"key\",\"type\":\"string\"}],\"name\":\"get\",\"outputs\":[{\"name\":\"\",\"type\":\"string\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"key\",\"type\":\"string\"},{\"name\":\"value\",\"type\":\"string\"}],\"name\":\"set\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]\n"
        val (key, value) = Pair("foo", "bar")
        val contract = deploy(KlaytnAccounts.from, byteCode).let {
            caver.contract.create(abi, it?.contractAddress)
        }

        val input = caver.abi.encodeFunctionCall(
            contract.getMethod("set"),
            listOf(key, value)
        )
        execution(KlaytnAccounts.from, contract.contractAddress, input)
        val result = contract.call("get", key).first().value

        assert(result == value)
    }

    @Test
    fun `NFT 토큰 발행 및 전송 테스트`() {
        caver.wallet.add(KlaytnAccounts.from)
        val contract = caver.kct.kip17.deploy(KlaytnAccounts.from.address, "TEST_NFT", "SYMBOL")
        val tokenId = BigInteger.valueOf(1)
        val tokenURI = "https://www.klaytn.com/"
        contract.mintWithTokenURI(
            KlaytnAccounts.to.address,
            tokenId,
            tokenURI,
            SendOptions(KlaytnAccounts.from.address)
        )
        assert(contract.tokenURI(tokenId) == tokenURI)
    }

    @Test
    fun `IPFS 이미지 파일 테스트`() {
        // 약 3mb 파일 기준 업로드 1초 이상, 다운로드 2초 이상
        caver.ipfs.setIPFSNode("ipfs.infura.io", 5001, true)
        val file = this::class.java.getResource("image.jpg")

        val cid = printRunTime("upload") {
            caver.ipfs.add(file.path).also { println("cid: $it") }
        }

        val content = printRunTime("download") {
            caver.ipfs.get(cid).also { println("file size: ${it.size}") }
        }

        assert(file.readBytes().size == content.size)
    }

    @Test
    fun `IPFS JSON 파일 테스트`() {
        // 약 4kb 파일 기준 업로드 약 0.2s, 다운로드 약 0.2s
        caver.ipfs.setIPFSNode("ipfs.infura.io", 5001, true)
        val file = this::class.java.getResource("sample.json")

        val cid = printRunTime("upload") {
            caver.ipfs.add(file.path).also { println("cid: $it") }
        }

        val content = printRunTime("download") {
            caver.ipfs.get(cid).also { println("file size: ${it.size}") }
        }

        assert(file.readBytes().size == content.size)
    }

    private fun <T> printRunTime(tag: String? = null, block: () -> T): T {
        val time = System.currentTimeMillis()
        return block().also {
            val sec = (System.currentTimeMillis() - time).toDouble() / 1000
            println("${tag?.let { "$tag: " }}$sec sec")
        }
    }
}
