package com.example.peoplenearby

import android.content.Context
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FirebaseHelper (context: Context) {

    private val db = FirebaseDatabase.getInstance().getReference("users")
    private val userId = UserSettings(context).getOrCreateUserId()

    fun uploadLocation(lat: Double, lon: Double) {

        val user = User(
            id = userId,
            name = "You",
            avatarUrl = "https://i.pravatar.cc/150?u=$userId",
            latitude = String.format("%.2f", lat).toDouble(),
            longitude = String.format("%.2f", lon).toDouble(),
            timestamp = System.currentTimeMillis()
        )

        db.child(userId).setValue(user)
    }

    fun listenNearbyUsers(
        onUsersUpdate: (List<User>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<User>()

                for (snap in snapshot.children) {
                    val user = snap.getValue(User::class.java)
                    if (user != null && user.id != userId) list.add(user)
                }

                onUsersUpdate(list)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.toException())
            }
        })
    }
}