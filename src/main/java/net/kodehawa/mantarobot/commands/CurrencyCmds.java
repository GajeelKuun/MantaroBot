package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.BanzyEnforcer;
import net.kodehawa.mantarobot.commands.currency.inventory.ItemStack;
import net.kodehawa.mantarobot.commands.currency.inventory.Items;
import net.kodehawa.mantarobot.commands.currency.inventory.TextChannelGround;
import net.kodehawa.mantarobot.data.Data.UserData;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.Async;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CurrencyCmds extends Module {
	public CurrencyCmds() {
		super(Category.CURRENCY);

		profile();
		loot();
		summon();
		gamble();
		richest();

		/*
		TODO NEXT:
		 - inventory command
		 - sell command
		 - transfer command
		 - mine command
		 */
	}

	private void gamble() {
		BanzyEnforcer banzyEnforcer = new BanzyEnforcer(2000);
		Random r = new Random();

		super.register("gamble", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				String id = event.getAuthor().getId();

				if (!banzyEnforcer.process(id)) {
					event.getChannel().sendMessage(":stopwatch:" +
						"Cooldown a lil bit, you're gambling so fast that I can't print enough money!").queue();
					return;
				}

				UserData user = MantaroData.getData().get().getUser(event.getAuthor(), true);

				if (user.money <= 0) {
					event.getChannel().sendMessage("\u274C You're broke. Search for some credits first!").queue();
					return;
				}

				double multiplier;
				long i;
				int luck;
				try {
					if (content.equals("all") || content.equals("everything")) {
						i = user.money;
						multiplier = 1.5d + (r.nextInt(1500) / 1000d);
						luck = 50 + (int) (multiplier * 10) + r.nextInt(20);
					} else {
						i = Integer.parseInt(content);
						if (i > user.money) throw new UnsupportedOperationException();
						multiplier = 1.2d + (i / user.money * r.nextInt(1300) / 1000d);
						luck = 15 + (int) (multiplier * 15) + r.nextInt(10);
					}
				} catch (NumberFormatException e) {
					event.getChannel().sendMessage("\u274C Please type a valid number equal or less than your credits or `all` to gamble all your credits.").queue();
					return;
				} catch (UnsupportedOperationException e) {
					event.getChannel().sendMessage("\u274C Please type a value within your credits amount.").queue();
					return;
				}

				if (luck > r.nextInt(100)) {
					int gains = (int) (i * multiplier);
					if (user.addMoney(gains)) {
						event.getChannel().sendMessage("\uD83C\uDFB2 Congrats, you won " + gains + " credits!").queue();
					} else {
						event.getChannel().sendMessage("\uD83C\uDFB2 Congrats, you won " + gains + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java integer. Here's a buggy money bag for you.").queue();
					}
				} else {
					user.money = Math.max(0, user.money - i);
					event.getChannel().sendMessage("\uD83C\uDFB2 Sadly, you lost " + (user.money == 0 ? "all your" : i) + " credits! \uD83D\uDE26").queue();
				}

				MantaroData.getData().update();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return null;
			}
		});
	}

	private void loot() {
		BanzyEnforcer banzyEnforcer = new BanzyEnforcer(5000);
		Random r = new Random();

		super.register("loot", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				String id = event.getAuthor().getId();

				if (!banzyEnforcer.process(id)) {
					event.getChannel().sendMessage(":stopwatch:" +
						"Cooldown a lil bit, you're ratelimited right now so maybe wait a little bit more and let other people loot.").queue();
					return;
				}

				UserData userData = MantaroData.getData().get().getUser(event.getAuthor(), true);
				List<ItemStack> loot = TextChannelGround.of(event).collect();
				int moneyFound = Math.max(0, r.nextInt(400) - 300);

				if (!loot.isEmpty()) {
					String s = ItemStack.toString(ItemStack.reduce(loot));
					userData.getInventory().merge(loot);
					if (moneyFound != 0) {
						if (userData.addMoney(moneyFound)) {
							event.getChannel().sendMessage("Digging through messages, you found " + s + ", along with " + moneyFound + " credits!").queue();
						} else {
							event.getChannel().sendMessage("Digging through messages, you found " + s + ", along with " + moneyFound + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java integer. Here's a buggy money bag for you.").queue();
						}
					} else {
						event.getChannel().sendMessage("Digging through messages, you found " + s).queue();
					}
				} else {
					if (moneyFound != 0) {
						if (userData.addMoney(moneyFound)) {
							event.getChannel().sendMessage("Digging through messages, you found " + moneyFound + " credits!").queue();
						} else {
							event.getChannel().sendMessage("Digging through messages, you found " + moneyFound + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java integer. Here's a buggy money bag for you.").queue();
						}
					} else {
						event.getChannel().sendMessage("Digging through messages, you found nothing but dust").queue();
					}
				}

				MantaroData.getData().update();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Loot command")
					.setDescription("Loots the current chat for items, for usage in Mantaro's currency system.\n"
						+ "Currently, there are ``" + Items.ALL.length + "`` items avaliable in chance," +
						"for which you have a random chance of getting one or more.")
					.addField("Usage", "~>loot", false)
					.build();
			}
		});
	}

	private void lottery() {
		//TODO @AdrianTodt re-do this with Currency system + Expirator
		List<User> users = new ArrayList<>();
		super.register("lottery", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				User author = event.getAuthor();
				if (!users.contains(author)) {
					event.getChannel().sendMessage("\uD83D\uDCAC " + "You won **" + new Random().nextInt(350) + "USD**, congrats!").queue();
					users.add(author);
				} else {
					event.getChannel().sendMessage("\uD83D\uDCAC " + "Try again later! (Usable every 24 hours)").queue();
				}
				Async.asyncSleepThen(86400000, () -> users.remove(author));
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "lottery")
					.setDescription("Retrieves a random amount of money.")
					.build();
			}
		});
	}

	private void profile() {
		super.register("profile", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				UserData data = MantaroData.getData().get().getUser(event.getAuthor(), false);
				event.getChannel().sendMessage(baseEmbed(event, event.getMember().getEffectiveName() + "'s Profile", event.getAuthor().getEffectiveAvatarUrl())
					.addField(":credit_card: Credits", "$ " + data.money, false)
					.addField(":pouch: Inventory", ItemStack.toString(data.getInventory().asList()), false)
					.build()
				).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Profile command.")
					.setDescription("Retrieves your current user profile.")
					.build();
			}
		});
	}

	private void richest() {
		super.register("richest", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				boolean global = !content.equals("guild");

				AtomicInteger integer = new AtomicInteger(1);
				event.getChannel().sendMessage(baseEmbed(event, global ? "Global richest Users" : "Guild richest Users", global ? event.getJDA().getSelfUser().getEffectiveAvatarUrl() : event.getGuild().getIconUrl())
					.setDescription(
						(global ? event.getJDA().getUsers().stream() : event.getGuild().getMembers().stream().map(Member::getUser)).filter(user -> !user.isBot())
							.sorted(Comparator.comparingLong(user -> Long.MAX_VALUE - MantaroData.getData().get().getUser(user, false).money))
							.limit(15)
							.map(user -> String.format("%d. **`%s#%s`** - **%d** Credits", integer.getAndIncrement(), user.getName(), user.getDiscriminator(), MantaroData.getData().get().getUser(user, false).money))
							.collect(Collectors.joining("\n"))
					)
					.build()
				).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return null;
			}
		});
	}

	//TODO Remove before release
	private void summon() {
		super.register("summon", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				Random r = new Random(System.currentTimeMillis());
				List<ItemStack> toDrop = ItemStack.stackfy(Stream.of(Items.ALL).filter(item -> r.nextBoolean()).collect(Collectors.toList()));
				TextChannelGround.of(event).drop(toDrop);
				event.getChannel().sendMessage("Dropped " + ItemStack.toString(toDrop) + " in the channel").queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return null;
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.BOT_OWNER;
			}
		});
	}
}
