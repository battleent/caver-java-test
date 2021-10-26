import com.klaytn.caver.Caver
import com.klaytn.caver.methods.response.TransactionReceipt
import com.klaytn.caver.transaction.AbstractFeeDelegatedTransaction
import com.klaytn.caver.transaction.AbstractTransaction
import com.klaytn.caver.transaction.TxPropertyBuilder
import com.klaytn.caver.transaction.response.PollingTransactionReceiptProcessor
import com.klaytn.caver.transaction.response.TransactionReceiptProcessor
import com.klaytn.caver.transaction.type.TransactionType
import com.klaytn.caver.utils.ChainId
import com.klaytn.caver.utils.CodeFormat
import com.klaytn.caver.wallet.keyring.AbstractKeyring
import com.klaytn.caver.wallet.keyring.RoleBasedKeyring
import okhttp3.Credentials
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClients
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.nio.charset.StandardCharsets

object CaverHelper {

    val caver: Caver by lazy {
        val httpService = HttpService(KlaytnProperties.url)
        val auth = Credentials.basic(
            KlaytnProperties.accessKeyId,
            KlaytnProperties.secretAccessKey,
            StandardCharsets.UTF_8
        )
        httpService.addHeader("Authorization", auth)
        httpService.addHeader("x-chain-id", ChainId.BAOBAB_TESTNET.toString())
        Caver(httpService)
    }

    private val defaultGas = BigInteger.valueOf(500_000)

    var receiptProcessor: TransactionReceiptProcessor = PollingTransactionReceiptProcessor(caver, 1000, 15)

    fun chargeKlay(address: String) {
        val httpclient = HttpClients.createDefault()
        val keyring = caver.wallet.keyring.generate()
        httpclient.execute(HttpPost("https://api-baobab.wallet.klaytn.com/faucet/run?address=${keyring.address}"))

        Thread.sleep(3000)

        val request = caver.rpc.klay.getBalance(keyring.address).send()
        transfer(keyring, address, request.value, true)
    }

    fun transfer(
        from: AbstractKeyring,
        to: String,
        value: BigInteger,
        feePayer: Boolean = true
    ): TransactionReceipt.TransactionReceiptData {
        return if (feePayer) {
            caver.transaction.feeDelegatedValueTransfer.create(
                TxPropertyBuilder.feeDelegatedValueTransfer()
                    .setFrom(from.address)
                    .setTo(to)
                    .setValue(value)
                    .setGas(defaultGas)
            )
        } else {
            caver.transaction.valueTransfer.create(
                TxPropertyBuilder.valueTransfer()
                    .setFrom(from.address)
                    .setTo(to)
                    .setValue(value)
                    .setGas(defaultGas)
            )
        }.send(from, feePayer)
    }

    fun updateAccount(
        keyring: AbstractKeyring,
        newKeyring: RoleBasedKeyring,
        feePayer: Boolean = true
    ): TransactionReceipt.TransactionReceiptData {
        return if (feePayer) {
            caver.transaction.feeDelegatedAccountUpdate.create(
                TxPropertyBuilder.feeDelegatedAccountUpdate()
                    .setFrom(keyring.address)
                    .setAccount(newKeyring.toAccount())
                    .setGas(defaultGas)
            )
        } else {
            caver.transaction.accountUpdate.create(
                TxPropertyBuilder.accountUpdate()
                    .setFrom(keyring.address)
                    .setAccount(newKeyring.toAccount())
                    .setGas(defaultGas)
            )
        }.send(keyring, feePayer)
    }

    fun deploy(
        keyring: AbstractKeyring,
        byteCode: String,
        feePayer: Boolean = true
    ): TransactionReceipt.TransactionReceiptData {
        return if (feePayer) {
            caver.transaction.feeDelegatedSmartContractDeploy.create(
                TxPropertyBuilder.feeDelegatedSmartContractDeploy()
                    .setFrom(keyring.address)
                    .setCodeFormat(Numeric.toHexStringWithPrefix(CodeFormat.EVM))
                    .setInput(byteCode)
                    .setGas(defaultGas)
            )
        } else {
            caver.transaction.smartContractDeploy.create(
                TxPropertyBuilder.smartContractDeploy()
                    .setFrom(keyring.address)
                    .setCodeFormat(Numeric.toHexStringWithPrefix(CodeFormat.EVM))
                    .setInput(byteCode)
                    .setGas(defaultGas)
            )
        }.send(keyring, feePayer)
    }

    fun execution(
        keyring: AbstractKeyring,
        contractAddress: String,
        input: String,
        feePayer: Boolean = true
    ): TransactionReceipt.TransactionReceiptData {
        return if (feePayer) {
            caver.transaction.feeDelegatedSmartContractExecution.create(
                TxPropertyBuilder.feeDelegatedSmartContractExecution()
                    .setFrom(keyring.address)
                    .setTo(contractAddress)
                    .setInput(input)
                    .setGas(defaultGas)
            )
        } else {
            caver.transaction.smartContractExecution.create(
                TxPropertyBuilder.smartContractExecution()
                    .setFrom(keyring.address)
                    .setTo(contractAddress)
                    .setInput(input)
                    .setGas(defaultGas)
            )
        }.send(keyring, feePayer)
    }

    private fun AbstractTransaction.send(
        keyring: AbstractKeyring,
        feePayer: Boolean
    ): TransactionReceipt.TransactionReceiptData {
        return this.let {
            it.sign(keyring)
        }.let {
            if (feePayer) {
                val valueTransfer = abstractFeeDelegatedTransaction(it)
                valueTransfer.feePayer = KlaytnAccounts.feePayer.address
                valueTransfer.klaytnCall = caver.rpc.klay
                valueTransfer.signAsFeePayer(KlaytnAccounts.feePayer)
            } else {
                it
            }
        }.let {
            caver.rpc.klay.sendRawTransaction(it.rawTransaction).send()
        }.let {
            receiptProcessor.waitForTransactionReceipt(it.result).also { receiptData ->
                println(receiptData.transactionHash)
            }
        }
    }

    private fun abstractFeeDelegatedTransaction(it: AbstractTransaction): AbstractFeeDelegatedTransaction {
        return when (it.type) {
            TransactionType.TxTypeFeeDelegatedValueTransfer.name ->
                caver.transaction.feeDelegatedValueTransfer.decode(it.rawTransaction)
            TransactionType.TxTypeFeeDelegatedAccountUpdate.name ->
                caver.transaction.feeDelegatedAccountUpdate.decode(it.rawTransaction)
            TransactionType.TxTypeFeeDelegatedSmartContractDeploy.name ->
                caver.transaction.feeDelegatedSmartContractDeploy.decode(it.rawTransaction)
            TransactionType.TxTypeFeeDelegatedSmartContractExecution.name ->
                caver.transaction.feeDelegatedSmartContractExecution.decode(it.rawTransaction)
            else -> throw Exception()
        } as AbstractFeeDelegatedTransaction
    }
}
