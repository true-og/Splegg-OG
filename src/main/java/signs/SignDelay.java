package signs;

import org.bukkit.block.Sign;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public class SignDelay implements Runnable {

	String[] data;
	Sign sign;

	public SignDelay(String[] data, Sign sign) {

		this.data = data;
		this.sign = sign;

	}

	public void run() {

		for(int i = 0; i < this.data.length; i++) {

			TextComponent signData = Component.text(this.data[i]);
			this.sign.line(i, signData);

		}

		this.sign.update();

	}

}