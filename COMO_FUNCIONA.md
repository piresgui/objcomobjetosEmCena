# Como funciona o Leitor OBJ

Este documento explica o que foi implementado no projeto e como cada parte funciona, com foco especial em **como a locomoção pela cena foi feita**.

O projeto é um visualizador 3D em Java/Swing que le varios arquivos `.obj`, posiciona os modelos lado a lado em uma cena e desenha tudo em wireframe (so as arestas dos triangulos, sem preenchimento), permitindo que o usuario ande livremente pela cena com o teclado e o mouse.

Nao ha nenhuma biblioteca grafica (OpenGL, LWJGL, JavaFX 3D etc.) envolvida - tudo e feito "na mao": as matrizes, a projecao em perspectiva e o desenho usam apenas `java.awt.Graphics2D`.

---

## 1. Estrutura do projeto

```
src/
  MainClass.java        ponto de entrada (cria a janela)
  MainCanvas.java        loop principal, leitura do teclado/mouse, desenho da cena
  core3d/
    Ponto3D.java          um ponto/vetor 3D (x, y, z, w)
    Mat4x4.java            matriz 4x4 (translacao, rotacao, multiplicacao)
    Camera.java            posicao da camera + calculo da matriz de view
  obj/
    ObjLoader.java         le um arquivo .obj e devolve um ObjModel
    ObjModel.java           vertices + faces lidos, com bounding box
    SceneObject.java        um modelo posicionado dentro da cena
```

A ideia por tras dessa separacao: `core3d` tem so matematica (pontos e matrizes, sem saber nada sobre `.obj`), `obj` sabe ler e organizar os modelos, e `MainCanvas` e o unico lugar que junta tudo pra desenhar na tela.

---

## 2. Carregando os `.obj`

### 2.1. Lendo o arquivo (`ObjLoader.java`)

Um arquivo `.obj` e um arquivo de texto onde cada linha descreve uma parte da malha 3D. Duas linhas importam pra esse projeto:

- `v x y z` -> declara um vertice (um ponto no espaco)
- `f i1 i2 i3 ...` -> declara uma face, ligando vertices ja declarados pelos seus indices (comecando em 1, nao em 0)

```java
if (linha.startsWith("v ") || linha.startsWith("v\t")) {
    String[] partes = linha.split("\\s+");
    float x = Float.parseFloat(partes[1]);
    float y = Float.parseFloat(partes[2]);
    float z = Float.parseFloat(partes[3]);
    vertices.add(new Ponto3D(x, y, z));
} else if (linha.startsWith("f ") || linha.startsWith("f\t")) {
    ...
}
```

Faces podem ter mais de 3 vertices (quads, pentagonos etc.), mas o resto do programa so sabe desenhar triangulos. Por isso, toda face e **triangulada em leque**: o primeiro vertice da face vira o vertice comum de todos os triangulos gerados.

```java
for (int i = 1; i < nVerts - 1; i++) {
    faces.add(new int[] { indices[0], indices[i], indices[i + 1] });
}
```

Por exemplo, uma face com vertices `[A, B, C, D]` vira dois triangulos: `(A, B, C)` e `(A, C, D)`.

Os indices no arquivo `.obj` tambem podem vir no formato `v/vt/vn` (vertice/textura/normal). Como o projeto nao usa textura nem normal do arquivo, so a primeira parte e aproveitada:

```java
private static int indiceVertice(String token, int totalVertices) {
    String parte = token.split("/")[0];
    int idx = Integer.parseInt(parte);
    if (idx < 0) {
        return totalVertices + idx; // indices negativos = relativos ao fim da lista
    }
    return idx - 1; // .obj comeca em 1, Java comeca em 0
}
```

### 2.2. Bounding box (`ObjModel.java`)

Depois de ler todos os vertices, `ObjModel` calcula a **caixa delimitadora** (bounding box) do modelo - os valores minimo e maximo de X, Y e Z. Isso e usado no proximo passo pra descobrir o tamanho real do modelo e centraliza-lo.

### 2.3. Posicionando o modelo na cena (`SceneObject.java`)

Os arquivos `.obj` da pasta vem em escalas completamente diferentes (uma cadeira pequena e um aviao gigante podem ter as mesmas unidades numericas). Pra que todos apareçam em um tamanho razoavel lado a lado, cada `SceneObject` normaliza o modelo:

```java
float maior = modelo.maiorDimensao();
float escala = maior > 0.0001f ? (tamanhoAlvo / maior) : 1f;

float cx = modelo.centroX();
float cy = modelo.minY; // apoia o modelo no "chao" em vez de centralizar no Y
float cz = modelo.centroZ();

verticesMundo = new Ponto3D[modelo.vertices.length];
for (int i = 0; i < modelo.vertices.length; i++) {
    Ponto3D v = modelo.vertices[i];
    float x = (v.x - cx) * escala + px;
    float y = (v.y - cy) * escala + py;
    float z = (v.z - cz) * escala + pz;
    verticesMundo[i] = new Ponto3D(x, y, z);
}
```

Em palavras: pega a maior dimensao do modelo (`maiorDimensao()`), calcula um fator de escala pra que essa dimensao vire `tamanhoAlvo` unidades, centraliza o modelo em X/Z, encosta ele no chao (Y = 0) usando o `minY`, e por fim desloca tudo pra posicao `(px, py, pz)` escolhida na cena. O resultado (`verticesMundo`) ja fica pronto em **coordenadas de mundo**, calculado uma unica vez no carregamento - a cena e estatica, so a camera se move, entao nao precisa recalcular isso a cada frame.

A lista de modelos e posicoes fica em `MainCanvas.carregarCena()`:

```java
Object[][] cena = {
        { "medieval house.obj", -18f, 9f },
        { "chair_01.obj", -10f, 2.5f },
        { "tank.obj", -5f, 4f },
        { "AIM120D.obj", 0f, 5f },
        { "Bench_LowRes.obj", 6f, 4f },
        { "mig21_fishbed.obj", 13f, 7f },
        { "SR71.obj", 22f, 9f },
        { "x-35_obj.obj", 32f, 7f },
};
```

Cada linha e `{ arquivo, posicaoX, tamanhoAlvo }`. Pra adicionar outro `.obj` na cena, basta adicionar uma linha nesse array.

---

## 3. A locomocao pela cena (o coracao do projeto)

A camera e representada pela classe `Camera`:

```java
public class Camera {
	public float x = 0, y = 2, z = -15;
	public float yaw = 0;
	public float pitch = 0;
	...
}
```

- `x, y, z` -> posicao da camera no mundo
- `yaw` -> rotacao horizontal (pra onde a camera esta "olhando" no plano X/Z), em graus
- `pitch` -> rotacao vertical (olhar pra cima/baixo), em graus

Essas quatro variaveis sao **tudo** que define onde a camera esta e pra onde ela olha. Andar pela cena = mudar `x/y/z`. Olhar ao redor = mudar `yaw/pitch`.

### 3.1. Capturando o input

`MainCanvas` registra um `KeyListener` que so liga/desliga flags booleanas quando uma tecla e pressionada ou solta - ele nao move a camera diretamente:

```java
private void trataTecla(int key, boolean pressionado) {
	switch (key) {
		case KeyEvent.VK_W: moveFrente = pressionado; break;
		case KeyEvent.VK_S: moveTras = pressionado; break;
		case KeyEvent.VK_A: moveEsquerda = pressionado; break;
		case KeyEvent.VK_D: moveDireita = pressionado; break;
		case KeyEvent.VK_SPACE: moveCima = pressionado; break;
		case KeyEvent.VK_SHIFT: moveBaixo = pressionado; break;
		case KeyEvent.VK_LEFT: olharEsquerda = pressionado; break;
		case KeyEvent.VK_RIGHT: olharDireita = pressionado; break;
		case KeyEvent.VK_UP: olharCima = pressionado; break;
		case KeyEvent.VK_DOWN: olharBaixo = pressionado; break;
	}
}
```

Esse padrao (guardar o estado da tecla em vez de agir na hora) e o que permite segurar `W` e andar continuamente, em vez de dar um "passo" a cada tecla pressionada.

O mouse funciona parecido, mas so precisa da diferenca de posicao entre um frame e outro (arrastando com o botao esquerdo):

```java
public void mouseDragged(MouseEvent e) {
	int dx = e.getX() - ultimoMouseX;
	int dy = e.getY() - ultimoMouseY;
	ultimoMouseX = e.getX();
	ultimoMouseY = e.getY();

	camera.yaw += dx * VEL_OLHAR_MOUSE;
	camera.pitch += dy * VEL_OLHAR_MOUSE;
	camera.clampPitch();
}
```

### 3.2. Vetores de direcao (pra onde e "frente"?)

Pra andar "pra frente", o programa precisa saber o que "frente" significa dependendo de pra onde a camera esta olhando (o `yaw`). Isso e calculado em `Camera.forwardXZ()` e `Camera.rightXZ()`:

```java
public float[] forwardXZ() {
	float rad = (float) Math.toRadians(yaw);
	return new float[] { -(float) Math.sin(rad), 0, (float) Math.cos(rad) };
}

public float[] rightXZ() {
	float rad = (float) Math.toRadians(yaw);
	return new float[] { (float) Math.cos(rad), 0, (float) Math.sin(rad) };
}
```

Com `yaw = 0`, `forwardXZ()` devolve `(0, 0, 1)` (andar reto no eixo Z) e `rightXZ()` devolve `(1, 0, 0)` (andar reto no eixo X). Conforme o `yaw` muda, esses vetores giram junto - e por isso que apertar `W` sempre anda "pra onde a camera esta olhando", nao numa direcao fixa do mundo. O calculo ignora o `pitch` de proposito: senao, olhar pra cima faria o personagem "voar" ao andar pra frente, o que nao e o comportamento esperado de um passeio pela cena.

### 3.3. Atualizando a posicao a cada frame (`simulaMundo`)

A cada frame, `simulaMundo(diftimeMs)` le as flags de tecla, soma os vetores de direcao correspondentes e move a camera:

```java
public void simulaMundo(long diftimeMs) {
	float dt = diftimeMs / 1000f;

	float[] frente = camera.forwardXZ();
	float[] direita = camera.rightXZ();

	float vx = 0, vy = 0, vz = 0;
	if (moveFrente) { vx += frente[0]; vz += frente[2]; }
	if (moveTras) { vx -= frente[0]; vz -= frente[2]; }
	if (moveDireita) { vx += direita[0]; vz += direita[2]; }
	if (moveEsquerda) { vx -= direita[0]; vz -= direita[2]; }
	if (moveCima) { vy += 1; }
	if (moveBaixo) { vy -= 1; }

	camera.x += vx * VEL_MOVIMENTO * dt;
	camera.y += vy * VEL_MOVIMENTO * dt;
	camera.z += vz * VEL_MOVIMENTO * dt;

	if (olharEsquerda) camera.yaw -= VEL_OLHAR_TECLADO * dt;
	if (olharDireita) camera.yaw += VEL_OLHAR_TECLADO * dt;
	if (olharCima) camera.pitch -= VEL_OLHAR_TECLADO * dt;
	if (olharBaixo) camera.pitch += VEL_OLHAR_TECLADO * dt;
	camera.clampPitch();
}
```

Pontos importantes:

- **`dt` (delta time):** o tempo em segundos desde o ultimo frame. Multiplicar a velocidade por `dt` faz a camera andar na mesma velocidade (em unidades por segundo) independente do FPS do computador - sem isso, o jogo andaria mais rapido em maquinas mais rapidas.
- **Diagonal nao fica mais rapida "errado":** apertar `W` e `D` juntos soma os dois vetores (`frente + direita`), entao anda na diagonal. Isso tecnicamente anda um pouco mais rapido que uma direcao so (a soma nao e normalizada), mas e simples o suficiente pra esse projeto.
- **`camera.clampPitch()`:** trava o `pitch` entre -89 e +89 graus, pra impedir que o jogador "vire de cabeca pra baixo" olhando demais pra cima ou pra baixo.

### 3.4. De volta a matematica: como o movimento da camera afeta o desenho

A camera anda mudando `x/y/z/yaw/pitch`, mas o que realmente aparece na tela e calculado transformando os vertices do mundo pro "espaco da camera" - ou seja, reescrevendo a posicao de cada vertice como se a camera estivesse parada na origem olhando pra frente. Isso e a **matriz de view**, montada em `Camera.getViewMatrix()`:

```java
public Mat4x4 getViewMatrix() {
	Mat4x4 t = new Mat4x4();
	t.setTranslate(-x, -y, -z);

	Mat4x4 ry = new Mat4x4();
	ry.setRotateY(-yaw);

	Mat4x4 rx = new Mat4x4();
	rx.setRotateX(-pitch);

	return rx.multiplica(ry).multiplica(t);
}
```

A logica: pra "desfazer" onde a camera esta e pra onde ela olha, aplica-se o inverso de cada transformacao, na ordem inversa:

1. `T(-x, -y, -z)` - translada o mundo inteiro na direcao oposta a posicao da camera (como se a camera fosse arrastada de volta pra origem)
2. `Ry(-yaw)` - desfaz a rotacao horizontal da camera
3. `Rx(-pitch)` - desfaz a rotacao vertical da camera

Essas tres matrizes sao multiplicadas em `rx.multiplica(ry).multiplica(t)`, formando uma unica matriz que faz as tres operacoes de uma vez quando aplicada a um ponto. A multiplicacao de matrizes em si e a operacao classica de algebra linear:

```java
public Mat4x4 multiplica(Mat4x4 outra) {
	Mat4x4 r = new Mat4x4();
	r.zera();
	for (int i = 0; i < 4; i++) {
		for (int j = 0; j < 4; j++) {
			float soma = 0;
			for (int k = 0; k < 4; k++) {
				soma += mat[i][k] * outra.mat[k][j];
			}
			r.mat[i][j] = soma;
		}
	}
	return r;
}
```

### 3.5. Desenhando com a nova posicao (loop de render)

A cada frame, `MainCanvas.paint()` pega a matriz de view atual (que reflete a posicao/rotacao mais recente da camera) e aplica em cada vertice de cada objeto da cena:

```java
Mat4x4 view = camera.getViewMatrix();
...
for (SceneObject obj : objetos) {
	Ponto3D[] verticesView = new Ponto3D[obj.verticesMundo.length];
	for (int i = 0; i < obj.verticesMundo.length; i++) {
		verticesView[i] = obj.verticesMundo[i].multiplicadoPor(view);
	}
	...
}
```

Depois de transformados pro espaco da camera, os vertices passam por uma **projecao em perspectiva** simples: quanto mais longe (maior `z`), menor o objeto aparece na tela.

```java
float focal = (H / 2f) / (float) Math.tan(Math.toRadians(FOV_GRAUS / 2f));
...
if (p0.z <= NEAR || p1.z <= NEAR || p2.z <= NEAR) continue; // ignora triangulos atras/perto demais da camera

float s0 = focal / p0.z;
int x0 = Math.round(W / 2f + p0.x * s0);
int y0 = Math.round(H / 2f - p0.y * s0);
```

`focal` e calculado a partir do campo de visao (`FOV_GRAUS`, 70 graus) - e a distancia (em pixels) que faria um objeto de tamanho 1 unidade a 1 unidade de distancia preencher a metade da tela. Dividir `focal` por `p.z` (a profundidade do ponto) da o efeito de perspectiva: pontos com `z` maior (mais longe) sao divididos por um numero maior e encolhem na tela.

Por fim, os tres pontos projetados de cada triangulo sao ligados com `g.drawLine`, desenhando so as arestas (modo wireframe):

```java
g.drawLine(x0, y0, x1, y1);
g.drawLine(x1, y1, x2, y2);
g.drawLine(x2, y2, x0, y0);
```

### 3.6. Resumo do ciclo de locomocao

```
tecla pressionada  ->  flag booleana liga (ex: moveFrente = true)
                              |
                              v
simulaMundo(dt)  ->  le as flags, calcula vetor de movimento,
                       atualiza camera.x/y/z/yaw/pitch
                              |
                              v
paint()  ->  monta a matriz de view a partir da camera atual,
              transforma os vertices do mundo pro espaco da camera,
              projeta em 2D e desenha as arestas dos triangulos
                              |
                              v
     tela atualizada mostrando a cena do ponto de vista novo
```

Esse ciclo roda em loop numa thread separada (`MainCanvas.run()`), redesenhando a cena a cada iteracao com `paintImmediately`, o que da a sensacao de movimento fluido enquanto as teclas ficam pressionadas.

---

## 4. Controles

| Tecla / acao | Efeito |
|---|---|
| `W` / `S` | Anda pra frente / pra tras |
| `A` / `D` | Anda pra esquerda / direita (strafe) |
| `Space` / `Shift` | Sobe / desce |
| Setas (`←` `→` `↑` `↓`) | Olha ao redor (yaw / pitch) |
| Botao esquerdo do mouse + arrastar | Olha ao redor |

## 5. Rodando o projeto

```bash
javac -d bin -encoding UTF-8 $(find src -name "*.java")
java -cp bin MainClass
```

Precisa rodar a partir da raiz do projeto, ja que os arquivos `.obj` sao carregados com caminho relativo (ex: `"tank.obj"`).
