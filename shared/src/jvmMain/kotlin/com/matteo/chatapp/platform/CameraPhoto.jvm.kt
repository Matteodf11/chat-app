package com.matteo.chatapp.platform

actual suspend fun flipCameraPhoto(file: PickedAttachmentFile): PickedAttachmentFile =
    error("La captura de cámara está disponible en Android")
