Tenemos 4 usuarios que quieren compartirse una contraseña de forma segura, ya que tienen el netflix comprado a mitades.

El cliente envía al servidor 3 cosas cuando se registra: (aunque el usuario solo tiene que meter el número de teléfono, el resto es transparente)
Cada usuario cuando se registra en la aplicación genera un par de claves pública-privada de curva elíptica (EC P-256).
Se guarda la clave privada en almacenamiento seguro en el móvil, se manda la clave pública al servidor.
El cliente además utiliza su número de teléfono  como identificador. Cuando llega el número de teléfono en plano al servidor, el servidor hashea con un hmac-sha-256 para así poder tener un indexado de usuario cercano a O(1), se utiliza un pepper para este hmac-sha-256.
El cliente por último envía una contraseña que se hashea con sha-256 en la app y se envía al servidor, para que nunca le llegue al servidor en texto plano. Cuando le llega la contraseña la encripta con argon2id, un algoritmo fuerte que incluye salt, además de con otro pepper distinto al del hmac-sha-256.

Hay que tener en cuenta también que la aplicación también funciona offline como password manager. Por lo que existe para cada usuario una master key que se guarda en el dispositivo de forma segura y no se manda a ningún lado. Esto se guarda cifrado con AES-256-GCM. Se usa EncryptedSharedPreferences para guardar de forma segura la key.  Con SecureRandom se usa generación random criptográfica.

En relación a la compartición de contraseñas hay un concepto en la app que es el de grupo, un grupo solo se puede usar cuando la aplicación está online.
El concepto alrededor del grupo es el SGK (Symmetric Group Key). Cada grupo tiene un SGK. Cuando un usuario crea un grupo este genera el SGK en la aplicación, lo encripta con la clave pública del creador mediante ECIES (ECDH + AES-256-GCM) y lo sube al servidor. (redundante pero no molesta)
Para cada usuario que quieras añadir obtienes su clave pública del servidor, cifras el SGK con esta clave pública usando ECIES. Subes el SGK cifrado al servidor, y el usuario que quiere entrar al grupo descifra la SGK cifrada con su clave privada. A partir de ahí ambos tienen el SGK en plano, por lo tanto todo lo que suban a ese grupo va a estar cifrado con AES con ese SGK.

Cuando el usuario pasa de estar en el vault de forma offline y quiere entrar a las funciones del grupo que son online el usuario no tiene que logearse con número y contraseña, ya que la contraseña se manda de forma opaca y totalmente transparente al usuario.
