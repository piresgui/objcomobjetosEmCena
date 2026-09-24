package obj;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import core3d.Ponto3D;

public class ObjLoader {

	public static ObjModel carregar(String caminho) throws IOException {
		List<Ponto3D> vertices = new ArrayList<>();
		List<int[]> faces = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader(caminho))) {
			String linha;
			while ((linha = br.readLine()) != null) {
				linha = linha.trim();
				if (linha.isEmpty() || linha.startsWith("#")) continue;

				if (linha.startsWith("v ") || linha.startsWith("v\t")) {
					String[] partes = linha.split("\\s+");
					float x = Float.parseFloat(partes[1]);
					float y = Float.parseFloat(partes[2]);
					float z = Float.parseFloat(partes[3]);
					vertices.add(new Ponto3D(x, y, z));
				} else if (linha.startsWith("f ") || linha.startsWith("f\t")) {
					String[] partes = linha.split("\\s+");
					int nVerts = partes.length - 1;
					if (nVerts < 3) continue;

					int[] indices = new int[nVerts];
					for (int i = 0; i < nVerts; i++) {
						indices[i] = indiceVertice(partes[i + 1], vertices.size());
					}

					for (int i = 1; i < nVerts - 1; i++) {
						faces.add(new int[] { indices[0], indices[i], indices[i + 1] });
					}
				}
			}
		}

		Ponto3D[] vArray = vertices.toArray(new Ponto3D[0]);
		int[][] fArray = faces.toArray(new int[0][]);
		return new ObjModel(vArray, fArray);
	}

	private static int indiceVertice(String token, int totalVertices) {
		String parte = token.split("/")[0];
		int idx = Integer.parseInt(parte);
		if (idx < 0) {
			return totalVertices + idx;
		}
		return idx - 1;
	}
}
