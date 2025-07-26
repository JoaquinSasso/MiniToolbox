package com.joasasso.minitoolbox.data

import java.util.Locale

data class FraseTraducida(
    val categoria: String,
    val fraseBase: String,
    val traducciones: Map<String, String>
)

data class Idioma(
    val nombre: String,
    val codigo: String, // Ej: "en", "fr"
    val locale: Locale
)


val idiomasDisponibles = listOf(
    Idioma("Inglés", "en", Locale("en")),
    Idioma("Francés", "fr", Locale("fr")),
    Idioma("Italiano", "it", Locale("it")),
    Idioma("Alemán", "de", Locale("de")),
    Idioma("Portugués", "pt", Locale("pt")),
    Idioma("Japonés", "ja", Locale("ja", "JP")),
    Idioma("Chino", "zh", Locale("zh", "CN")),
    Idioma("Coreano", "ko", Locale("ko", "KR"))
)


val frasesIngles = listOf(
    FraseTraducida("Saludos", "Hola", mapOf("en" to "Hello")),
    FraseTraducida("Saludos", "Buenos días", mapOf("en" to "Good morning")),
    FraseTraducida("Saludos", "Buenas tardes", mapOf("en" to "Good afternoon")),
    FraseTraducida("Saludos", "Buenas noches", mapOf("en" to "Good evening")),
    FraseTraducida("Saludos", "Adiós", mapOf("en" to "Goodbye")),
    FraseTraducida("Saludos", "Hasta luego", mapOf("en" to "See you later")),
    FraseTraducida("Saludos", "Mucho gusto", mapOf("en" to "Nice to meet you")),

    FraseTraducida("Preguntas básicas", "¿Cómo estás?", mapOf("en" to "How are you?")),
    FraseTraducida("Preguntas básicas", "¿Qué hora es?", mapOf("en" to "What time is it?")),
    FraseTraducida("Preguntas básicas", "¿Cuánto cuesta?", mapOf("en" to "How much does it cost?")),
    FraseTraducida("Preguntas básicas", "¿Dónde está el baño?", mapOf("en" to "Where is the bathroom?")),
    FraseTraducida("Preguntas básicas", "¿Habla español?", mapOf("en" to "Do you speak Spanish?")),
    FraseTraducida("Preguntas básicas", "¿Puede ayudarme?", mapOf("en" to "Can you help me?")),

    FraseTraducida("Emergencias", "¡Ayuda!", mapOf("en" to "Help!")),
    FraseTraducida("Emergencias", "Llame a la policía", mapOf("en" to "Call the police")),
    FraseTraducida("Emergencias", "Necesito un médico", mapOf("en" to "I need a doctor")),
    FraseTraducida("Emergencias", "Estoy perdido", mapOf("en" to "I'm lost")),
    FraseTraducida("Emergencias", "Me siento mal", mapOf("en" to "I feel sick")),
    FraseTraducida("Emergencias", "Es una emergencia", mapOf("en" to "It's an emergency")),

    FraseTraducida("Transporte", "¿Dónde está la estación?", mapOf("en" to "Where is the station?")),
    FraseTraducida("Transporte", "¿A qué hora sale el tren?", mapOf("en" to "What time does the train leave?")),
    FraseTraducida("Transporte", "Quisiera un boleto", mapOf("en" to "I would like a ticket")),
    FraseTraducida("Transporte", "¿Cuánto cuesta el pasaje?", mapOf("en" to "How much is the fare?")),
    FraseTraducida("Transporte", "Este es mi destino", mapOf("en" to "This is my destination")),

    FraseTraducida("Comida", "Tengo hambre", mapOf("en" to "I'm hungry")),
    FraseTraducida("Comida", "Tengo sed", mapOf("en" to "I'm thirsty")),
    FraseTraducida("Comida", "Quisiera una mesa para dos", mapOf("en" to "I'd like a table for two")),
    FraseTraducida("Comida", "¿Cuál es el menú?", mapOf("en" to "What is the menu?")),
    FraseTraducida("Comida", "Sin picante, por favor", mapOf("en" to "No spicy, please")),
    FraseTraducida("Comida", "La cuenta, por favor", mapOf("en" to "The check, please")),

    FraseTraducida("Otros", "Gracias", mapOf("en" to "Thank you")),
    FraseTraducida("Otros", "De nada", mapOf("en" to "You're welcome")),
    FraseTraducida("Otros", "Perdón", mapOf("en" to "Excuse me")),
    FraseTraducida("Otros", "Lo siento", mapOf("en" to "I'm sorry")),
    FraseTraducida("Otros", "No entiendo", mapOf("en" to "I don't understand")),
    FraseTraducida("Otros", "¿Puede repetirlo?", mapOf("en" to "Can you repeat that?"))
)


val frasesFrances = listOf(
    FraseTraducida("Saludos", "Hola", mapOf("fr" to "Bonjour")),
    FraseTraducida("Saludos", "Buenos días", mapOf("fr" to "Bonjour")),
    FraseTraducida("Saludos", "Buenas tardes", mapOf("fr" to "Bon après-midi")),
    FraseTraducida("Saludos", "Buenas noches", mapOf("fr" to "Bonsoir")),
    FraseTraducida("Saludos", "Adiós", mapOf("fr" to "Au revoir")),
    FraseTraducida("Saludos", "Hasta luego", mapOf("fr" to "À plus tard")),
    FraseTraducida("Saludos", "Mucho gusto", mapOf("fr" to "Enchanté")),

    FraseTraducida("Preguntas básicas", "¿Cómo estás?", mapOf("fr" to "Comment ça va ?")),
    FraseTraducida("Preguntas básicas", "¿Qué hora es?", mapOf("fr" to "Quelle heure est-il ?")),
    FraseTraducida("Preguntas básicas", "¿Cuánto cuesta?", mapOf("fr" to "Combien ça coûte ?")),
    FraseTraducida("Preguntas básicas", "¿Dónde está el baño?", mapOf("fr" to "Où sont les toilettes ?")),
    FraseTraducida("Preguntas básicas", "¿Habla español?", mapOf("fr" to "Parlez-vous espagnol ?")),
    FraseTraducida("Preguntas básicas", "¿Puede ayudarme?", mapOf("fr" to "Pouvez-vous m'aider ?")),

    FraseTraducida("Emergencias", "¡Ayuda!", mapOf("fr" to "À l'aide !")),
    FraseTraducida("Emergencias", "Llame a la policía", mapOf("fr" to "Appelez la police")),
    FraseTraducida("Emergencias", "Necesito un médico", mapOf("fr" to "J'ai besoin d'un médecin")),
    FraseTraducida("Emergencias", "Estoy perdido", mapOf("fr" to "Je suis perdu")),
    FraseTraducida("Emergencias", "Me siento mal", mapOf("fr" to "Je ne me sens pas bien")),
    FraseTraducida("Emergencias", "Es una emergencia", mapOf("fr" to "C'est une urgence")),

    FraseTraducida("Transporte", "¿Dónde está la estación?", mapOf("fr" to "Où est la gare ?")),
    FraseTraducida("Transporte", "¿A qué hora sale el tren?", mapOf("fr" to "À quelle heure part le train ?")),
    FraseTraducida("Transporte", "Quisiera un boleto", mapOf("fr" to "Je voudrais un billet")),
    FraseTraducida("Transporte", "¿Cuánto cuesta el pasaje?", mapOf("fr" to "Combien coûte le billet ?")),
    FraseTraducida("Transporte", "Este es mi destino", mapOf("fr" to "C'est ma destination")),

    FraseTraducida("Comida", "Tengo hambre", mapOf("fr" to "J'ai faim")),
    FraseTraducida("Comida", "Tengo sed", mapOf("fr" to "J'ai soif")),
    FraseTraducida("Comida", "Quisiera una mesa para dos", mapOf("fr" to "Je voudrais une table pour deux")),
    FraseTraducida("Comida", "¿Cuál es el menú?", mapOf("fr" to "Quel est le menu ?")),
    FraseTraducida("Comida", "Sin picante, por favor", mapOf("fr" to "Sans épices, s'il vous plaît")),
    FraseTraducida("Comida", "La cuenta, por favor", mapOf("fr" to "L'addition, s'il vous plaît")),

    FraseTraducida("Otros", "Gracias", mapOf("fr" to "Merci")),
    FraseTraducida("Otros", "De nada", mapOf("fr" to "De rien")),
    FraseTraducida("Otros", "Perdón", mapOf("fr" to "Pardon")),
    FraseTraducida("Otros", "Lo siento", mapOf("fr" to "Je suis désolé")),
    FraseTraducida("Otros", "No entiendo", mapOf("fr" to "Je ne comprends pas")),
    FraseTraducida("Otros", "¿Puede repetirlo?", mapOf("fr" to "Pouvez-vous répéter ?"))
)

val frasesItaliano = listOf(
    FraseTraducida("Saludos", "Hola", mapOf("it" to "Ciao")),
    FraseTraducida("Saludos", "Buenos días", mapOf("it" to "Buongiorno")),
    FraseTraducida("Saludos", "Buenas tardes", mapOf("it" to "Buon pomeriggio")),
    FraseTraducida("Saludos", "Buenas noches", mapOf("it" to "Buonasera")),
    FraseTraducida("Saludos", "Adiós", mapOf("it" to "Arrivederci")),
    FraseTraducida("Saludos", "Hasta luego", mapOf("it" to "A dopo")),
    FraseTraducida("Saludos", "Mucho gusto", mapOf("it" to "Piacere di conoscerti")),

    FraseTraducida("Preguntas básicas", "¿Cómo estás?", mapOf("it" to "Come stai?")),
    FraseTraducida("Preguntas básicas", "¿Qué hora es?", mapOf("it" to "Che ore sono?")),
    FraseTraducida("Preguntas básicas", "¿Cuánto cuesta?", mapOf("it" to "Quanto costa?")),
    FraseTraducida("Preguntas básicas", "¿Dónde está el baño?", mapOf("it" to "Dov'è il bagno?")),
    FraseTraducida("Preguntas básicas", "¿Habla español?", mapOf("it" to "Parla spagnolo?")),
    FraseTraducida("Preguntas básicas", "¿Puede ayudarme?", mapOf("it" to "Può aiutarmi?")),

    FraseTraducida("Emergencias", "¡Ayuda!", mapOf("it" to "Aiuto!")),
    FraseTraducida("Emergencias", "Llame a la policía", mapOf("it" to "Chiami la polizia")),
    FraseTraducida("Emergencias", "Necesito un médico", mapOf("it" to "Ho bisogno di un medico")),
    FraseTraducida("Emergencias", "Estoy perdido", mapOf("it" to "Mi sono perso")),
    FraseTraducida("Emergencias", "Me siento mal", mapOf("it" to "Mi sento male")),
    FraseTraducida("Emergencias", "Es una emergencia", mapOf("it" to "È un'emergenza")),

    FraseTraducida("Transporte", "¿Dónde está la estación?", mapOf("it" to "Dov'è la stazione?")),
    FraseTraducida("Transporte", "¿A qué hora sale el tren?", mapOf("it" to "A che ora parte il treno?")),
    FraseTraducida("Transporte", "Quisiera un boleto", mapOf("it" to "Vorrei un biglietto")),
    FraseTraducida("Transporte", "¿Cuánto cuesta el pasaje?", mapOf("it" to "Quanto costa il biglietto?")),
    FraseTraducida("Transporte", "Este es mi destino", mapOf("it" to "Questa è la mia destinazione")),

    FraseTraducida("Comida", "Tengo hambre", mapOf("it" to "Ho fame")),
    FraseTraducida("Comida", "Tengo sed", mapOf("it" to "Ho sete")),
    FraseTraducida("Comida", "Quisiera una mesa para dos", mapOf("it" to "Vorrei un tavolo per due")),
    FraseTraducida("Comida", "¿Cuál es el menú?", mapOf("it" to "Qual è il menù?")),
    FraseTraducida("Comida", "Sin picante, por favor", mapOf("it" to "Senza piccante, per favore")),
    FraseTraducida("Comida", "La cuenta, por favor", mapOf("it" to "Il conto, per favore")),

    FraseTraducida("Otros", "Gracias", mapOf("it" to "Grazie")),
    FraseTraducida("Otros", "De nada", mapOf("it" to "Prego")),
    FraseTraducida("Otros", "Perdón", mapOf("it" to "Scusa")),
    FraseTraducida("Otros", "Lo siento", mapOf("it" to "Mi dispiace")),
    FraseTraducida("Otros", "No entiendo", mapOf("it" to "Non capisco")),
    FraseTraducida("Otros", "¿Puede repetirlo?", mapOf("it" to "Può ripetere?"))
)

val frasesAleman = listOf(
    FraseTraducida("Saludos", "Hola", mapOf("de" to "Hallo")),
    FraseTraducida("Saludos", "Buenos días", mapOf("de" to "Guten Morgen")),
    FraseTraducida("Saludos", "Buenas tardes", mapOf("de" to "Guten Tag")),
    FraseTraducida("Saludos", "Buenas noches", mapOf("de" to "Guten Abend")),
    FraseTraducida("Saludos", "Adiós", mapOf("de" to "Auf Wiedersehen")),
    FraseTraducida("Saludos", "Hasta luego", mapOf("de" to "Bis später")),
    FraseTraducida("Saludos", "Mucho gusto", mapOf("de" to "Freut mich, Sie kennenzulernen")),

    FraseTraducida("Preguntas básicas", "¿Cómo estás?", mapOf("de" to "Wie geht es Ihnen?")),
    FraseTraducida("Preguntas básicas", "¿Qué hora es?", mapOf("de" to "Wie spät ist es?")),
    FraseTraducida("Preguntas básicas", "¿Cuánto cuesta?", mapOf("de" to "Wie viel kostet das?")),
    FraseTraducida("Preguntas básicas", "¿Dónde está el baño?", mapOf("de" to "Wo ist die Toilette?")),
    FraseTraducida("Preguntas básicas", "¿Habla español?", mapOf("de" to "Sprechen Sie Spanisch?")),
    FraseTraducida("Preguntas básicas", "¿Puede ayudarme?", mapOf("de" to "Können Sie mir helfen?")),

    FraseTraducida("Emergencias", "¡Ayuda!", mapOf("de" to "Hilfe!")),
    FraseTraducida("Emergencias", "Llame a la policía", mapOf("de" to "Rufen Sie die Polizei")),
    FraseTraducida("Emergencias", "Necesito un médico", mapOf("de" to "Ich brauche einen Arzt")),
    FraseTraducida("Emergencias", "Estoy perdido", mapOf("de" to "Ich habe mich verirrt")),
    FraseTraducida("Emergencias", "Me siento mal", mapOf("de" to "Ich fühle mich schlecht")),
    FraseTraducida("Emergencias", "Es una emergencia", mapOf("de" to "Es ist ein Notfall")),

    FraseTraducida("Transporte", "¿Dónde está la estación?", mapOf("de" to "Wo ist der Bahnhof?")),
    FraseTraducida("Transporte", "¿A qué hora sale el tren?", mapOf("de" to "Wann fährt der Zug ab?")),
    FraseTraducida("Transporte", "Quisiera un boleto", mapOf("de" to "Ich hätte gern eine Fahrkarte")),
    FraseTraducida("Transporte", "¿Cuánto cuesta el pasaje?", mapOf("de" to "Wie viel kostet das Ticket?")),
    FraseTraducida("Transporte", "Este es mi destino", mapOf("de" to "Das ist mein Ziel")),

    FraseTraducida("Comida", "Tengo hambre", mapOf("de" to "Ich habe Hunger")),
    FraseTraducida("Comida", "Tengo sed", mapOf("de" to "Ich habe Durst")),
    FraseTraducida("Comida", "Quisiera una mesa para dos", mapOf("de" to "Ich hätte gern einen Tisch für zwei")),
    FraseTraducida("Comida", "¿Cuál es el menú?", mapOf("de" to "Was gibt es auf der Speisekarte?")),
    FraseTraducida("Comida", "Sin picante, por favor", mapOf("de" to "Bitte nicht scharf")),
    FraseTraducida("Comida", "La cuenta, por favor", mapOf("de" to "Die Rechnung, bitte")),

    FraseTraducida("Otros", "Gracias", mapOf("de" to "Danke")),
    FraseTraducida("Otros", "De nada", mapOf("de" to "Gern geschehen")),
    FraseTraducida("Otros", "Perdón", mapOf("de" to "Entschuldigung")),
    FraseTraducida("Otros", "Lo siento", mapOf("de" to "Es tut mir leid")),
    FraseTraducida("Otros", "No entiendo", mapOf("de" to "Ich verstehe nicht")),
    FraseTraducida("Otros", "¿Puede repetirlo?", mapOf("de" to "Können Sie das wiederholen?"))
)

val frasesPortugues = listOf(
    FraseTraducida("Saludos", "Hola", mapOf("pt" to "Olá")),
    FraseTraducida("Saludos", "Buenos días", mapOf("pt" to "Bom dia")),
    FraseTraducida("Saludos", "Buenas tardes", mapOf("pt" to "Boa tarde")),
    FraseTraducida("Saludos", "Buenas noches", mapOf("pt" to "Boa noite")),
    FraseTraducida("Saludos", "Adiós", mapOf("pt" to "Adeus")),
    FraseTraducida("Saludos", "Hasta luego", mapOf("pt" to "Até logo")),
    FraseTraducida("Saludos", "Mucho gusto", mapOf("pt" to "Muito prazer")),

    FraseTraducida("Preguntas básicas", "¿Cómo estás?", mapOf("pt" to "Como vai?")),
    FraseTraducida("Preguntas básicas", "¿Qué hora es?", mapOf("pt" to "Que horas são?")),
    FraseTraducida("Preguntas básicas", "¿Cuánto cuesta?", mapOf("pt" to "Quanto custa?")),
    FraseTraducida("Preguntas básicas", "¿Dónde está el baño?", mapOf("pt" to "Onde fica o banheiro?")),
    FraseTraducida("Preguntas básicas", "¿Habla español?", mapOf("pt" to "Você fala espanhol?")),
    FraseTraducida("Preguntas básicas", "¿Puede ayudarme?", mapOf("pt" to "Você pode me ajudar?")),

    FraseTraducida("Emergencias", "¡Ayuda!", mapOf("pt" to "Socorro!")),
    FraseTraducida("Emergencias", "Llame a la policía", mapOf("pt" to "Chame a polícia")),
    FraseTraducida("Emergencias", "Necesito un médico", mapOf("pt" to "Preciso de um médico")),
    FraseTraducida("Emergencias", "Estoy perdido", mapOf("pt" to "Estou perdido")),
    FraseTraducida("Emergencias", "Me siento mal", mapOf("pt" to "Estou me sentindo mal")),
    FraseTraducida("Emergencias", "Es una emergencia", mapOf("pt" to "É uma emergência")),

    FraseTraducida("Transporte", "¿Dónde está la estación?", mapOf("pt" to "Onde fica a estação?")),
    FraseTraducida("Transporte", "¿A qué hora sale el tren?", mapOf("pt" to "Que horas o trem parte?")),
    FraseTraducida("Transporte", "Quisiera un boleto", mapOf("pt" to "Gostaria de uma passagem")),
    FraseTraducida("Transporte", "¿Cuánto cuesta el pasaje?", mapOf("pt" to "Quanto custa a passagem?")),
    FraseTraducida("Transporte", "Este es mi destino", mapOf("pt" to "Este é meu destino")),

    FraseTraducida("Comida", "Tengo hambre", mapOf("pt" to "Estou com fome")),
    FraseTraducida("Comida", "Tengo sed", mapOf("pt" to "Estou com sede")),
    FraseTraducida("Comida", "Quisiera una mesa para dos", mapOf("pt" to "Gostaria de uma mesa para dois")),
    FraseTraducida("Comida", "¿Cuál es el menú?", mapOf("pt" to "Qual é o cardápio?")),
    FraseTraducida("Comida", "Sin picante, por favor", mapOf("pt" to "Sem pimenta, por favor")),
    FraseTraducida("Comida", "La cuenta, por favor", mapOf("pt" to "A conta, por favor")),

    FraseTraducida("Otros", "Gracias", mapOf("pt" to "Obrigado")),
    FraseTraducida("Otros", "De nada", mapOf("pt" to "De nada")),
    FraseTraducida("Otros", "Perdón", mapOf("pt" to "Desculpe")),
    FraseTraducida("Otros", "Lo siento", mapOf("pt" to "Sinto muito")),
    FraseTraducida("Otros", "No entiendo", mapOf("pt" to "Não entendo")),
    FraseTraducida("Otros", "¿Puede repetirlo?", mapOf("pt" to "Pode repetir?"))
)

val frasesJapones = listOf(
    FraseTraducida("Saludos", "Hola", mapOf("ja" to "Kon'nichiwa")),
    FraseTraducida("Saludos", "Buenos días", mapOf("ja" to "Ohayō gozaimasu")),
    FraseTraducida("Saludos", "Buenas tardes", mapOf("ja" to "Konnichiwa")),
    FraseTraducida("Saludos", "Buenas noches", mapOf("ja" to "Konbanwa")),
    FraseTraducida("Saludos", "Adiós", mapOf("ja" to "Sayōnara")),
    FraseTraducida("Saludos", "Hasta luego", mapOf("ja" to "Mata ne")),
    FraseTraducida("Saludos", "Mucho gusto", mapOf("ja" to "Hajimemashite")),

    FraseTraducida("Preguntas básicas", "¿Cómo estás?", mapOf("ja" to "Ogenki desu ka?")),
    FraseTraducida("Preguntas básicas", "¿Qué hora es?", mapOf("ja" to "Ima nanji desu ka?")),
    FraseTraducida("Preguntas básicas", "¿Cuánto cuesta?", mapOf("ja" to "Ikura desu ka?")),
    FraseTraducida("Preguntas básicas", "¿Dónde está el baño?", mapOf("ja" to "Toire wa doko desu ka?")),
    FraseTraducida("Preguntas básicas", "¿Habla español?", mapOf("ja" to "Supeingo o hanasemasu ka?")),
    FraseTraducida("Preguntas básicas", "¿Puede ayudarme?", mapOf("ja" to "Tetsudatte kuremasu ka?")),

    FraseTraducida("Emergencias", "¡Ayuda!", mapOf("ja" to "Tasukete!")),
    FraseTraducida("Emergencias", "Llame a la policía", mapOf("ja" to "Keisatsu o yonde kudasai")),
    FraseTraducida("Emergencias", "Necesito un médico", mapOf("ja" to "Isha ga hitsuyō desu")),
    FraseTraducida("Emergencias", "Estoy perdido", mapOf("ja" to "Mayoimashita")),
    FraseTraducida("Emergencias", "Me siento mal", mapOf("ja" to "Kibun ga warui desu")),
    FraseTraducida("Emergencias", "Es una emergencia", mapOf("ja" to "Kinkyū jitai desu")),

    FraseTraducida("Transporte", "¿Dónde está la estación?", mapOf("ja" to "Eki wa doko desu ka?")),
    FraseTraducida("Transporte", "¿A qué hora sale el tren?", mapOf("ja" to "Densha wa nanji ni demasu ka?")),
    FraseTraducida("Transporte", "Quisiera un boleto", mapOf("ja" to "Kippu o kudasai")),
    FraseTraducida("Transporte", "¿Cuánto cuesta el pasaje?", mapOf("ja" to "Ryōkin wa ikura desu ka?")),
    FraseTraducida("Transporte", "Este es mi destino", mapOf("ja" to "Koko ga watashi no mokuteki desu")),

    FraseTraducida("Comida", "Tengo hambre", mapOf("ja" to "Onaka ga sukimashita")),
    FraseTraducida("Comida", "Tengo sed", mapOf("ja" to "Nodo ga kawakimashita")),
    FraseTraducida("Comida", "Quisiera una mesa para dos", mapOf("ja" to "Futari yō no tēburu o onegai shimasu")),
    FraseTraducida("Comida", "¿Cuál es el menú?", mapOf("ja" to "Menyū wa nan desu ka?")),
    FraseTraducida("Comida", "Sin picante, por favor", mapOf("ja" to "Karai mono wa nashi de onegai shimasu")),
    FraseTraducida("Comida", "La cuenta, por favor", mapOf("ja" to "Okanjō o onegai shimasu")),

    FraseTraducida("Otros", "Gracias", mapOf("ja" to "Arigatō gozaimasu")),
    FraseTraducida("Otros", "De nada", mapOf("ja" to "Dōitashimashite")),
    FraseTraducida("Otros", "Perdón", mapOf("ja" to "Sumimasen")),
    FraseTraducida("Otros", "Lo siento", mapOf("ja" to "Gomen nasai")),
    FraseTraducida("Otros", "No entiendo", mapOf("ja" to "Wakarimasen")),
    FraseTraducida("Otros", "¿Puede repetirlo?", mapOf("ja" to "Mō ichido itte kudasai"))
)

val frasesChino = listOf(
    FraseTraducida("Saludos", "Hola", mapOf("zh" to "Nǐ hǎo")),
    FraseTraducida("Saludos", "Buenos días", mapOf("zh" to "Zǎoshang hǎo")),
    FraseTraducida("Saludos", "Buenas tardes", mapOf("zh" to "Xiàwǔ hǎo")),
    FraseTraducida("Saludos", "Buenas noches", mapOf("zh" to "Wǎn'ān")),
    FraseTraducida("Saludos", "Adiós", mapOf("zh" to "Zàijiàn")),
    FraseTraducida("Saludos", "Hasta luego", mapOf("zh" to "Dāihuì jiàn")),
    FraseTraducida("Saludos", "Mucho gusto", mapOf("zh" to "Hěn gāoxìng rènshí nǐ")),

    FraseTraducida("Preguntas básicas", "¿Cómo estás?", mapOf("zh" to "Nǐ hǎo ma?")),
    FraseTraducida("Preguntas básicas", "¿Qué hora es?", mapOf("zh" to "Xiànzài jǐ diǎn?")),
    FraseTraducida("Preguntas básicas", "¿Cuánto cuesta?", mapOf("zh" to "Duōshǎo qián?")),
    FraseTraducida("Preguntas básicas", "¿Dónde está el baño?", mapOf("zh" to "Xǐshǒujiān zài nǎlǐ?")),
    FraseTraducida("Preguntas básicas", "¿Habla español?", mapOf("zh" to "Nǐ huì shuō xībānyá yǔ ma?")),
    FraseTraducida("Preguntas básicas", "¿Puede ayudarme?", mapOf("zh" to "Nǐ kěyǐ bāng wǒ ma?")),

    FraseTraducida("Emergencias", "¡Ayuda!", mapOf("zh" to "Jiùmìng!")),
    FraseTraducida("Emergencias", "Llame a la policía", mapOf("zh" to "Qǐng jiào jǐngchá")),
    FraseTraducida("Emergencias", "Necesito un médico", mapOf("zh" to "Wǒ xūyào yīshēng")),
    FraseTraducida("Emergencias", "Estoy perdido", mapOf("zh" to "Wǒ mílù le")),
    FraseTraducida("Emergencias", "Me siento mal", mapOf("zh" to "Wǒ bù shūfu")),
    FraseTraducida("Emergencias", "Es una emergencia", mapOf("zh" to "Zhè shì jǐnjí qíngkuàng")),

    FraseTraducida("Transporte", "¿Dónde está la estación?", mapOf("zh" to "Huǒchē zhàn zài nǎlǐ?")),
    FraseTraducida("Transporte", "¿A qué hora sale el tren?", mapOf("zh" to "Huǒchē jǐ diǎn kāi?")),
    FraseTraducida("Transporte", "Quisiera un boleto", mapOf("zh" to "Wǒ xiǎng mǎi yī zhāng piào")),
    FraseTraducida("Transporte", "¿Cuánto cuesta el pasaje?", mapOf("zh" to "Chéngkè fèi duōshǎo?")),
    FraseTraducida("Transporte", "Este es mi destino", mapOf("zh" to "Zhè shì wǒ de mùdìdì")),

    FraseTraducida("Comida", "Tengo hambre", mapOf("zh" to "Wǒ è le")),
    FraseTraducida("Comida", "Tengo sed", mapOf("zh" to "Wǒ kě le")),
    FraseTraducida("Comida", "Quisiera una mesa para dos", mapOf("zh" to "Qǐng gěi wǒ yī zhāng liǎng rén de zhuōzi")),
    FraseTraducida("Comida", "¿Cuál es el menú?", mapOf("zh" to "Càidān shì shénme?")),
    FraseTraducida("Comida", "Sin picante, por favor", mapOf("zh" to "Qǐng búyào là de")),
    FraseTraducida("Comida", "La cuenta, por favor", mapOf("zh" to "Qǐng jiézhàng")),

    FraseTraducida("Otros", "Gracias", mapOf("zh" to "Xièxiè")),
    FraseTraducida("Otros", "De nada", mapOf("zh" to "Bù kèqì")),
    FraseTraducida("Otros", "Perdón", mapOf("zh" to "Duìbùqǐ")),
    FraseTraducida("Otros", "Lo siento", mapOf("zh" to "Bàoqiàn")),
    FraseTraducida("Otros", "No entiendo", mapOf("zh" to "Wǒ bù míngbái")),
    FraseTraducida("Otros", "¿Puede repetirlo?", mapOf("zh" to "Qǐng zài shuō yībiàn"))
)

val frasesCoreano = listOf(
    FraseTraducida("Saludos", "Hola", mapOf("ko" to "Annyeonghaseyo")),
    FraseTraducida("Saludos", "Buenos días", mapOf("ko" to "Joitnalibnida")),
    FraseTraducida("Saludos", "Buenas tardes", mapOf("ko" to "Joaheun ohuibnida")),
    FraseTraducida("Saludos", "Buenas noches", mapOf("ko" to "Jal jayo")),
    FraseTraducida("Saludos", "Adiós", mapOf("ko" to "Annyeonghi gaseyo")),
    FraseTraducida("Saludos", "Hasta luego", mapOf("ko" to "Najung-e bwaeyo")),
    FraseTraducida("Saludos", "Mucho gusto", mapOf("ko" to "Mannaseo bangapseumnida")),

    FraseTraducida("Preguntas básicas", "¿Cómo estás?", mapOf("ko" to "Eotteohge jinaeseyo?")),
    FraseTraducida("Preguntas básicas", "¿Qué hora es?", mapOf("ko" to "Myeot si ibnikka?")),
    FraseTraducida("Preguntas básicas", "¿Cuánto cuesta?", mapOf("ko" to "Eolmaibnikka?")),
    FraseTraducida("Preguntas básicas", "¿Dónde está el baño?", mapOf("ko" to "Hwajangsil-eodiibnikka?")),
    FraseTraducida("Preguntas básicas", "¿Habla español?", mapOf("ko" to "Seupein-eoleul halsu iss-eoyo?")),
    FraseTraducida("Preguntas básicas", "¿Puede ayudarme?", mapOf("ko" to "Dowajusigess-eoyo?")),

    FraseTraducida("Emergencias", "¡Ayuda!", mapOf("ko" to "Dowajuseyo!")),
    FraseTraducida("Emergencias", "Llame a la policía", mapOf("ko" to "Gyeongchal-eul bureujuseyo")),
    FraseTraducida("Emergencias", "Necesito un médico", mapOf("ko" to "Uisaga pil-yohaeyo")),
    FraseTraducida("Emergencias", "Estoy perdido", mapOf("ko" to "Gilgeul ilheoss-eoyo")),
    FraseTraducida("Emergencias", "Me siento mal", mapOf("ko" to "Geunyang an joeun gibunibnida")),
    FraseTraducida("Emergencias", "Es una emergencia", mapOf("ko" to "Geos-i gung-geubhabnida")),

    FraseTraducida("Transporte", "¿Dónde está la estación?", mapOf("ko" to "Yeog-eodi ibnikka?")),
    FraseTraducida("Transporte", "¿A qué hora sale el tren?", mapOf("ko" to "Gicha neun myeot si-e chulbalhaeyo?")),
    FraseTraducida("Transporte", "Quisiera un boleto", mapOf("ko" to "Pyo hana juseyo")),
    FraseTraducida("Transporte", "¿Cuánto cuesta el pasaje?", mapOf("ko" to "Yos-eun eolmaibnikka?")),
    FraseTraducida("Transporte", "Este es mi destino", mapOf("ko" to "Yeog-i nae mogjeogjiibnida")),

    FraseTraducida("Comida", "Tengo hambre", mapOf("ko" to "Baegopayo")),
    FraseTraducida("Comida", "Tengo sed", mapOf("ko" to "Mogmalla")),
    FraseTraducida("Comida", "Quisiera una mesa para dos", mapOf("ko" to "Dubeon jari juseyo")),
    FraseTraducida("Comida", "¿Cuál es el menú?", mapOf("ko" to "Menyu-ga mwoyeyo?")),
    FraseTraducida("Comida", "Sin picante, por favor", mapOf("ko" to "Maepge haji maseyo")),
    FraseTraducida("Comida", "La cuenta, por favor", mapOf("ko" to "Gyesanseo juseyo")),

    FraseTraducida("Otros", "Gracias", mapOf("ko" to "Gamsahamnida")),
    FraseTraducida("Otros", "De nada", mapOf("ko" to "Cheonmaneyo")),
    FraseTraducida("Otros", "Perdón", mapOf("ko" to "Joesonghamnida")),
    FraseTraducida("Otros", "Lo siento", mapOf("ko" to "Mianhamnida")),
    FraseTraducida("Otros", "No entiendo", mapOf("ko" to "Ihaega an dwaeyo")),
    FraseTraducida("Otros", "¿Puede repetirlo?", mapOf("ko" to "Dasi hanbeon malhaejuseyo"))
)

val todasLasFrases = frasesIngles + frasesFrances + frasesItaliano + frasesAleman +
        frasesPortugues + frasesJapones + frasesChino + frasesCoreano


val categoriasDisponibles = listOf(
    "Saludos",
    "Preguntas básicas",
    "Emergencias",
    "Transporte",
    "Comida",
    "Otros"
)


