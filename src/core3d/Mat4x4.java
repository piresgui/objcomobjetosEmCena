package core3d;

public class Mat4x4 {
	float mat[][] = new float[4][4];

	public Mat4x4() {
		setIdentity();
	}

	public void setIdentity() {
		zera();
		mat[0][0] = 1;
		mat[1][1] = 1;
		mat[2][2] = 1;
		mat[3][3] = 1;
	}

	public void zera() {
		for (int y = 0; y < 4; y++) {
			for (int x = 0; x < 4; x++) {
				mat[y][x] = 0;
			}
		}
	}

	public void setTranslate(float a, float b, float c) {
		setIdentity();
		mat[0][3] = a;
		mat[1][3] = b;
		mat[2][3] = c;
	}

	public void setEscala(float a, float b, float c) {
		zera();
		mat[0][0] = a;
		mat[1][1] = b;
		mat[2][2] = c;
		mat[3][3] = 1;
	}

	public void setRotateY(float angGraus) {
		zera();
		float rad = (float) Math.toRadians(angGraus);
		float sin = (float) Math.sin(rad);
		float cos = (float) Math.cos(rad);

		mat[0][0] = cos;
		mat[0][2] = -sin;

		mat[1][1] = 1;

		mat[2][0] = sin;
		mat[2][2] = cos;

		mat[3][3] = 1;
	}

	public void setRotateX(float angGraus) {
		zera();
		float rad = (float) Math.toRadians(angGraus);
		float sin = (float) Math.sin(rad);
		float cos = (float) Math.cos(rad);

		mat[0][0] = 1;

		mat[1][1] = cos;
		mat[1][2] = -sin;

		mat[2][1] = sin;
		mat[2][2] = cos;

		mat[3][3] = 1;
	}

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
}
