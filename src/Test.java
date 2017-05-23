import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Test {
	public static void main(String[] args) throws InterruptedException, IOException {
		File file = new File("nodeIpAddresses");

		BufferedWriter bw = new BufferedWriter(new FileWriter(file));

		for (int i = 0; i < 10; i++) {
			bw.write("127.0.0.1 " + (5000 + i));
			bw.newLine();
		}
		bw.close();

		BufferedReader br = new BufferedReader(new FileReader(file));
		while (br.ready()) {
			String[] split = br.readLine().split(" ");
			int portNumber = Integer.parseInt(split[1]);
			new Thread(new Node(file.getPath(), portNumber)).start();
		}

		br.close();
	}
}
