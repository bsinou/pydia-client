package org.sinou.android.pydia.services

import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.pydio.cells.api.*
import com.pydio.cells.transport.CellsTransport
import com.pydio.cells.transport.StateID
import com.pydio.cells.transport.auth.Token
import com.pydio.cells.transport.auth.credentials.JWTCredentials
import com.pydio.cells.transport.auth.jwt.IdToken
import com.pydio.cells.utils.MemoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.sinou.android.pydia.room.account.RAccount
import org.sinou.android.pydia.room.account.AccountDB
import org.sinou.android.pydia.room.account.RLiveSession
import org.sinou.android.pydia.room.account.RSession
import org.sinou.android.pydia.utils.AndroidCustomEncoder

/**
 * AccountService is the single source of truth for accounts, sessions and auth in the app.
 * It takes care of both local caching of session info and authentication against the remote
 * servers. It holds a SessionFactory that gives access to the SDK client
 */
class AccountService(val accountDB: AccountDB, private val workingDir: String?) {

    private val encoder: CustomEncoder = AndroidCustomEncoder()

    private val TAG = "AccountRepository"

    // Local stores to cache live objects
    private val servers = MemoryStore<Server>()
    private val transports = MemoryStore<Transport>()

    // Expose the session factory for clients to retrieve clients
    val sessionFactory: SessionFactory = SessionFactory(accountDB, servers, transports)

    // Temporary keep track of all states that have been generated for OAuth processes
    val inProcessCallbacks = mutableMapOf<String, ServerURL>()

    val liveSessions: LiveData<List<RLiveSession>> = accountDB.liveSessionDao().getLiveSessions()

    fun registerAccount(serverURL: ServerURL, credentials: Credentials): String? {
        try {

            val state = StateID(credentials.username, serverURL.toString());
            val existingAccount =
                accountDB.accountDao().getAccount(credentials.username, serverURL.toString())

            sessionFactory.registerAccountCredentials(serverURL, credentials)
            val server: Server = sessionFactory.getServer(serverURL.id)
            val account = toAccount(credentials.username, server)

            if (existingAccount == null) { // creation
                accountDB.accountDao().insert(account)
            } else { // update
                accountDB.accountDao().update(account)
            }
            registerLocalSession(StateID(account.username, account.url).id)

            return state.id

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    suspend fun forgetAccount(accountID: String) {
        try {
            accountDB.sessionDao().forgetSession(accountID)
            // retrieve account and delete by uid if found.
            accountDB.accountDao().forgetAccount(accountID)
       } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Stores a new row in the Session DB */
    fun registerLocalSession(accountID: String) {
        var newSession = accountDB.sessionDao().getSession(accountID)
        // if the session already exists, do nothing
        if (newSession == null) {
            newSession = fromAccountID(accountID)
            accountDB.sessionDao().insert(newSession)
        }
    }

    suspend fun openSession(accountID: String) {
        return withContext(Dispatchers.IO) {

            // First put other opened sessions in the background
            val tmpSessions = accountDB.sessionDao().foregroundSessions()
            for (currSession in tmpSessions) {
                currSession.lifecycleState = "background"
                accountDB.sessionDao().update(currSession)
            }

            var openSession = accountDB.sessionDao().getSession(accountID)

            if (openSession == null) {
                // TODO handle base dir
                openSession = fromAccountID(accountID)
                accountDB.sessionDao().insert(openSession)
            } else {
                openSession.lifecycleState = "foreground"
                accountDB.sessionDao().update(openSession)
            }
        }
    }

    /** Cells Credential flow management */
    suspend fun handleOAuthResponse(oauthState: String, code: String) {
        withContext(Dispatchers.IO) {
            val serverURL = inProcessCallbacks.get(oauthState)
            if (serverURL != null) {
                try {
                    val transport =
                        sessionFactory.getAnonymousTransport(serverURL.getId()) as CellsTransport
                    val token = transport.getTokenFromCode(code, encoder)
                    manageRetrievedToken(transport, oauthState, token)
                } catch (e: Exception) {
                    Log.e(TAG, "Could not finalize credential auth flow")
                    e.printStackTrace()
                }
            } else {                // Ignore callback
                Log.i(TAG, "Ignored call back with unknown state: ${oauthState}")
            }
        }
    }

    private fun manageRetrievedToken(transport: CellsTransport, oauthState: String, token: Token) {
        val idToken = IdToken.parse(encoder, token.idToken)
        val accountID = StateID(idToken.claims.name, transport.server.url())
        val jwtCredentials = JWTCredentials(accountID.username, token)

        registerAccount(transport.server.serverURL, jwtCredentials)

        // TODO: also launch:
        //   - workspace refresh
        //   - offline check and update (in case of configuration change)

        // This will directly try to use the newly registered session to get a Client
//        val client: Client = sf.getUnlockedClient(accountID.id)
//        val workspaces: MutableMap<String, WorkspaceNode> = HashMap()
//        client.workspaceList { ws: Node ->
//            workspaces[(ws as WorkspaceNode).slug] = ws
//        }
//        val account: AccountRecord = sf.getSession(accountID.id).getAccount()
//        account.setWorkspaces(workspaces)
//        App.getAccountService().updateAccount(account)
//        App.getSessionFactory().loadKnownAccounts()


        // TODO ?
        // Set the session as current in the app
        // Adapt poll and tasks
        // check if it was a background thread
        // redirectToCallerWithNewState(State.fromAccountId(accountID.id), oauthState)
    }


    // TODO there is more idiomatic way to do, see "behind the scene" codelab
    fun toAccount(username: String, server: Server): RAccount {
        return RAccount(
            accountID = StateID(username, server.url()).accountId,
            username = username,
            url = server.url(),
            serverLabel = server.label,
            skipVerify = server.serverURL.skipVerify(),
            isLegacy = server.isLegacy,
            welcomeMessage = server.getWelcomeMessage(),
            // FIXME define the correct auth status possible:
            // typically: no-credentials, expired, refreshing, conected
            authStatus = "connected",
        )
    }

    private fun fromAccountID(accountID: String): RSession {
        val newSession = RSession(
            accountID = accountID,
            baseDir = "/tmp",
            lifecycleState = "foreground",
            workspaces = listOf(),
            offlineRoots = listOf(),
            bookmarkCache = listOf(),
            shareCache = listOf(),
        )
        return newSession
    }

    //    public void createLocalFolders(String accountID) throws SDKException {
//
//        String baseFolderPath = baseLocalFolderPath(accountID);
//        File file = new File(baseFolderPath);
//        if (!file.exists() && !file.mkdirs()) {
//            throw new SDKException("could not create session base directory");
//        }
//
//        String cacheFolderPath = cacheLocalFolderPath(accountID);
//        file = new File(cacheFolderPath);
//        if (!file.exists() && !file.mkdirs()) {
//            throw new SDKException("could not create cache directory");
//        }
//
//        String tempFolderPath = tempLocalFolderPath(accountID);
//        file = new File(tempFolderPath);
//        if (!file.exists() && !file.mkdirs()) {
//            throw new SDKException("could not create temp directory");
//        }
//    }

    @WorkerThread
    suspend fun insert(account: RAccount) {
        accountDB.accountDao().insert(account)
    }

    @WorkerThread
    suspend fun update(account: RAccount) {
        accountDB.accountDao().update(account)
    }
}