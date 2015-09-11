package me.fru1t.rsbot.common.script.rt6;

import java.util.ArrayList;
import java.util.List;

import org.powerbot.script.rt6.ClientContext;
import org.powerbot.script.rt6.Item;

import me.fru1t.common.annotations.Inject;
import me.fru1t.common.annotations.Singleton;
import me.fru1t.rsbot.common.framework.components.Persona;
import me.fru1t.rsbot.common.framework.util.Condition;

/**
 * Provides utility methods for interacting with the RS3 backpack.
 */
// TODO(v1 cleanup): Find instances of BackpackUtil and convert to Backpack.
@Singleton
public class Backpack {
	private static final int MAX_BACKPACK_SIZE = 28;

	private final ClientContext ctx;
	private final Mouse mouseUtil;
	private final Persona persona;

	@Inject
	public Backpack(
			@Singleton ClientContext ctx,
			@Singleton Mouse mouseUtil,
			@Singleton Persona persona) {
		this.ctx = ctx;
		this.mouseUtil = mouseUtil;
		this.persona = persona;
	}

	/**
	 * Returns whether or not the backpack is full. Resets the Backpack query.
	 * @return Whether or not the backpack is full.
	 */
	public boolean isFull() {
		return ctx.backpack.select().size() == MAX_BACKPACK_SIZE;
	}

	/**
	 * Left clicks an amount of items within the inventory. If the amount specified is greater than
	 * the amount in the backpack, the amount will automatically be set to the number in count
	 * in the backpack. This includes 0 and will not throw an error.
	 *
	 * @param id The id of the items to click.
	 * @param amount The number of items to click.
	 * @return Returns true once all items have been clicked. Otherwise, false for any reason
	 * including errors.
	 */
	public boolean clickMultipleItemsWithSingleId(int id, int amount) {
		amount = ctx.backpack.select().id(id).count() < amount ? ctx.backpack.count() : amount;
		List<Item> items = new ArrayList<Item>();
		ctx.backpack.limit(amount).addTo(items);

		while (!items.isEmpty()) {
			for (int i = 0; i < items.size(); i++) {
				// TODO: Check if interactions re-open backpack.
				if (ctx.backpack.collapsed()) {
					return false;
				}

				if (!items.get(i).valid()) {
					items.remove(i);
				}

				if (!items.get(i).inViewport()) {
					ctx.backpack.scroll(items.get(i));
				}

				mouseUtil.click(items.get(i));

				Condition.sleep(persona.getNextInteractDelay());
			}
			// TODO: Maybe add a delay before looping again?
		}
		return true;
	}
}
