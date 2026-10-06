package uz.ishvaqtim.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uz.ishvaqtim.app.data.ProfileDao
import uz.ishvaqtim.app.data.ProfileEntity
import uz.ishvaqtim.app.nfc.CardHasher
import uz.ishvaqtim.app.remote.SupabaseClient

/** Ekran holatini saqlaydi: profil, oxirgi skan qilingan karta, NFC holati. */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = ProfileDao(app)

    var loading by mutableStateOf(true)
        private set

    var profile by mutableStateOf<ProfileEntity?>(null)
        private set

    var scannedCardHash by mutableStateOf<String?>(null)
        private set

    var nfcStatus by mutableStateOf("")

    init {
        viewModelScope.launch {
            profile = dao.get()
            loading = false
        }
    }

    var supabaseStatus by mutableStateOf("")

    fun onCardScanned(uid: ByteArray) {
        val hash = CardHasher.hash(uid)
        scannedCardHash = hash

        supabaseStatus = "Yuborilmoqda..."
        viewModelScope.launch {
            val ok = SupabaseClient.insertNfcScan(hash)
            supabaseStatus = if (ok) {
                "✅ Supabase'ga yuborildi"
            } else {
                "⚠️ Yuborilmadi (internet yoki server xatosi)"
            }
        }
    }

    fun saveProfile(newProfile: ProfileEntity) {
        viewModelScope.launch {
            dao.save(newProfile)
            profile = newProfile
        }
    }

    fun deleteProfile() {
        viewModelScope.launch {
            dao.clear()
            profile = null
        }
    }
}
