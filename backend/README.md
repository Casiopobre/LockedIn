Necesito que actues como arquitecto de software senior.

Estoy preparando el backend de una aplicación de vault de contraseñas. Vamos a diseñar un backend en Fast API con PostgresSQL, para que se encarge de la compartición de contraseñas entre usuarios.

Vamos a ver algunos casos de uso:
REGISTER

El usuario se registra con su ID y una contrasezña ya hasheada en sha-256. Esto permite identificarlo para hacer peticiones una vez logeado. Usaremos JWT para mantener esta identificación.
Además, el usuario enviará su clave pública al servidor, y este la almacenará, para que otros usuarios puedan pedirla, cifrar contraseñas con esta clave pública, y avisar al usuario que tiene contraseñas para sincronizar.

El backend, dado este ID, va a hashear argon2 y con un pepper secreto guardado en un .env del servidor. Entonces, resumen de la DB: Se guarda el ID hasheado con argon2 y peper, la pub key y la contraseña ya hasheada con SHA256.


LOGIN
El usuario envia el ID y la contraseña hasheada, y si coincide, el backend devuelve un JWT para persistir la sesión.


COMPARTIR CONTRASEÑAS
El usuario1 que quiere compartir una contraseña, crea un grupo, en el que se meterán más usuarios.
El primer paso es crear el grupo, al cual se le genera una contraseña de acceso con random.org. Esta contraseña será la Symmetric Group Key (SGK). Luego, el usuario1 pide la clave pública del usuario2, cifra la SGK con su clave pública, y la sube al servidor. Por último, el usuario2 recibe SGK cifrada, y la descifra con su clave privada para acceder al grupo. Ahora, usuario2, posee la SGK, por lo que puede acceder a las contraseñas del grupo.

Para compartir una contraseña a un grupo, el usuario1 sube la contraseña cifrada con la SGK, y el usuario2, la recibe cifrada y la descifra con la SGK.
