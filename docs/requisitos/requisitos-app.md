# Documento de Requisitos de Software (DRS)

**Projeto:** Urban Science AI  
**Versão:** 1.1 (MVP – Thin Client & Movimento Inteligente)  
**Data:** 17 de Agosto de 2026  
**Status:** Aprovado para Desenvolvimento  

---

## 1. Visão Geral do Sistema

O **Urban Science AI** é uma solução móvel voltada para o monitoramento urbano e relato automatizado de descarte irregular de resíduos sólidos em vias públicas. O sistema opera de forma integrada aos óculos inteligentes **Meta Ray-Ban** e adota a arquitetura **Thin Client (Cliente Ultraleve)**.

O smartphone do usuário atua exclusivamente como ponte de coleta e transmissão de dados com consumo de bateria otimizado:
- **Detecção de Movimento Passiva:** O app só aciona sensores de imagem e GPS quando o usuário está em deslocamento ativo na rua.
- **Processamento 100% Remoto:** Não há execução de visão computacional ou modelos pesados de Machine Learning no celular.
- **Feedback Hands-Free:** Confirmações de sucesso ou avisos de falha são reproduzidos diretamente em áudio nos alto-falantes dos óculos.

---

## 2. Premissas de Eficiência Energética e Arquitetura

1. **Arquitetura Thin Client:** Eliminação de qualquer biblioteca de processamento de imagem/IA embarcada para garantir compatibilidade com smartphones básicos e poupar bateria/memória RAM.
2. **Ciclo de Sono / Suspensão (*Sleep Mode*):** Em estado de repouso (ex.: usuário dentro de casa ou parado no trabalho), o app suspende qualquer rotina de captura ou verificação de câmera.
3. **GPS Sob Demanda:** O rádio de geolocalização não fica monitorando continuamente em alta precisão; o sinal de satélite é requisitado apenas no milissegundo de disparo da imagem.
4. **Despacho Direto via HTTP:** A foto capturada é empacotada com coordenadas brutas e enviada diretamente para a API remota via requisição `multipart/form-data`.

---

## 3. Requisitos Funcionais (RF)

### [RF01] Integração e Conexão com Meta Ray-Ban
* O aplicativo deve parear e sincronizar com os óculos inteligentes Meta Ray-Ban via Bluetooth/SDK oficial.
* O app deve monitorar o estado da conexão e direcionar a reprodução sonora para os alto-falantes integrados da armação.

### [RF02] Ativação por Reconhecimento de Movimento (*Activity Recognition*)
* O sistema deve utilizar APIs nativas de baixo consumo energético (*Google Activity Recognition* / *Core Motion*) para identificar se o usuário está em movimento (caminhando, correndo ou pedalando).
* O módulo de captura automática deve permanecer inativo (*idle/sleep*) enquanto o usuário estiver parado.

### [RF03] Modos de Acionamento da Captura
* **Modo Automático:** Quando o deslocamento for confirmado, o sistema efetua capturas periódicas em intervalos configuráveis sem necessidade de toques na tela.
* **Modo Manual:** O usuário pode disparar uma captura instantânea a qualquer momento através de um botão no app ou botão físico/atalho dos óculos.

### [RF04] Coleta de Geolocalização e Metadados
* No momento da captura da imagem, o aplicativo deve capturar:
  - Latitude e Longitude em graus decimais.
  - Precisão do GPS (em metros).
  - Data e hora exatas da captura (*timestamp* ISO 8601).
  - Tipo de acionamento (`AUTOMATIC` ou `MANUAL`).

### [RF05] Transmissão para a API Remota
* O aplicativo deve realizar uma requisição HTTP `POST` para o endpoint `/reports` enviando a imagem e os metadados.
* O aplicativo deve aguardar a resposta da API (síncrona ou assíncrona) para determinar o feedback sonoro.

### [RF06] Feedback em Áudio (*Audio Output*)
* O aplicativo deve sintetizar e reproduzir áudio nos óculos Meta Ray-Ban informando o resultado da operação:
  - **Sucesso (201 Created):** Mensagem curta (ex.: *"Registro enviado com sucesso"*).
  - **Falha (4xx/5xx ou Sem Conexão):** Mensagem explicativa (ex.: *"Falha no envio. Registro salvo localmente"* ou *"Erro de conexão"*).

### [RF07] Histórico de Envios
* O aplicativo deve fornecer uma tela simples de histórico contendo:
  - Miniatura da foto enviada.
  - Horário e data do envio.
  - Coordenadas geográficas.
  - Status do envio retornado pela API.

---

## 4. Requisitos Não-Funcionais (RNF)

* **[RNF01] Leveza da Aplicação (*Thin Client*):** O pacote de instalação (APK/IPA) não deve conter bibliotecas pesadas de IA/ML, minimizando uso de disco e RAM.
* **[RNF02] Máxima Eficiência Energética:** O consumo em segundo plano deve ser inferior a 2% de bateria por hora de uso em trajeto ativo.
* **[RNF03] Latência do Feedback:** O retorno sonoro deve ser emitido em até 3 segundos após o envio em redes 4G/5G estáveis.
* **[RNF04] Resiliência de Conectividade:** Caso ocorra *timeout* ou oscilação de rede móvel, a requisição não deve travar a interface do usuário.
* **[RNF05] Compatibilidade Ampla:** Suporte garantido a smartphones Android (API 26+) e iOS (15+) de especificações básicas e intermediárias.

---

## 5. Especificação do Contrato de API (REST)

**Base URL:** `https://api.urbanscience.ai/v1`  
**Autenticação:** `Bearer <token_jwt>` (via Header `Authorization`)

---

### 5.1. Envio de Registro de Ocorrência

* **Método:** `POST`
* **Rota:** `/reports`
* **Content-Type:** `multipart/form-data`

#### Headers da Requisição
| Header | Valor | Obrigatório |
| :--- | :--- | :--- |
| `Authorization` | `Bearer <token_jwt>` | Sim |
| `Content-Type` | `multipart/form-data` | Sim |
| `Accept` | `application/json` | Sim |

#### Parâmetros do Payload (`multipart/form-data`)
| Campo | Tipo | Descrição | Obrigatório |
| :--- | :--- | :--- | :--- |
| `image` | `File (Binary)` | Imagem em `.jpg`, `.jpeg` ou `.png` (Max: 5MB) | Sim |
| `latitude` | `Number` | Latitude em graus decimais (ex.: `-8.047562`) | Sim |
| `longitude` | `Number` | Longitude em graus decimais (ex.: `-34.877014`) | Sim |
| `accuracy` | `Number` | Precisão do GPS em metros (ex.: `4.5`) | Não |
| `timestamp` | `String` | Formato ISO 8601 UTC (ex.: `2026-08-17T13:40:00Z`) | Sim |
| `trigger_type` | `String` | Origem do disparo: `"AUTOMATIC"` ou `"MANUAL"` | Sim |
| `device_id` | `String` | Identificador do dispositivo/óculos | Não |

#### Respostas da API

##### 201 Created (Sucesso no Envio)
```json
{
  "status": "success",
  "message": "Registro recebido e enfileirado para processamento.",
  "data": {
    "report_id": "rep_9f8c2b1e-7a3d-4c5e-b812-192837465abc",
    "created_at": "2026-08-17T13:40:02Z",
    "status": "QUEUED",
    "audio_feedback": "Registro enviado com sucesso."
  }
}
```

##### 400 Bad Request (Dados Inválidos)
```json
{
  "status": "error",
  "code": "INVALID_PAYLOAD",
  "message": "Campos obrigatórios ausentes ou inválidos.",
  "errors": [
    {
      "field": "latitude",
      "message": "Latitude fora do intervalo permitido (-90 a 90)."
    }
  ],
  "audio_feedback": "Erro ao enviar. Dados de localização inválidos."
}
```

##### 401 Unauthorized (Autenticação Inválida)
```json
{
  "status": "error",
  "code": "UNAUTHORIZED",
  "message": "Token de autenticação ausente ou expirado.",
  "audio_feedback": "Erro de autenticação no aplicativo."
}
```

##### 422 Unprocessable Entity (Falha no Arquivo)
```json
{
  "status": "error",
  "code": "INVALID_IMAGE",
  "message": "O arquivo de imagem enviado está corrompido ou em formato não suportado.",
  "audio_feedback": "Falha na imagem capturada."
}
```

##### 500 Internal Server Error (Instabilidade no Servidor)
```json
{
  "status": "error",
  "code": "SERVER_ERROR",
  "message": "Instabilidade temporária nos servidores municipais.",
  "audio_feedback": "Servidor indisponível. Tente novamente mais tarde."
}
```

---

### 5.2. Consulta de Histórico de Envios

* **Método:** `GET`
* **Rota:** `/reports`
* **Query Parameters:** `page` (int, default: 1), `limit` (int, default: 20)

#### Resposta 200 OK
```json
{
  "status": "success",
  "page": 1,
  "limit": 20,
  "total": 45,
  "data": [
    {
      "id": "rep_9f8c2b1e-7a3d-4c5e-b812-192837465abc",
      "thumbnail_url": "https://storage.urbanscience.ai/thumbs/rep_9f8c2b1e.jpg",
      "latitude": -8.047562,
      "longitude": -34.877014,
      "trigger_type": "AUTOMATIC",
      "status": "PROCESSED",
      "detection_result": "GARBAGE_CONFIRMED",
      "created_at": "2026-08-17T13:40:02Z"
    }
  ]
}
```

---

## 6. Fluxo Lógico de Execução

```text
       +------------------------------------+
       |   Usuário em Deslocamento Ativo    |
       | (Identificado via Acelerômetro)    |
       +-----------------+------------------+
                         |
                         v
       +-----------------+------------------+
       | Disparo de Captura (Auto / Manual) |
       +-----------------+------------------+
                         |
                         v
       +-----------------+------------------+
       |   Obtenção da Foto + GPS Celular   |
       +-----------------+------------------+
                         |
                         v
       +-----------------+------------------+
       |  Upload Multipart para POST /reports |
       +-----------------+------------------+
                         |
            +------------+------------+
            |                         |
     (Status 201)              (Status 4xx/5xx)
            |                         |
            v                         v
   [Áudio Óculos: "Sucesso"]   [Áudio Óculos: "Erro"]
```
