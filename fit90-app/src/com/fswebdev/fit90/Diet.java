package com.fswebdev.fit90;

/**
 * Budget Indian diet: about 120 g protein and 2,400–2,550 kcal (maintenance for a
 * 70 kg active adult) for at most ₹100 a day. Totals are computed from the
 * ingredient table below, so changing a price or quantity keeps them honest.
 */
public final class Diet {

    /** Nutrition and price per 100 g/ml, or per piece when {@code unit} is "pc". */
    public static final class Food {
        public final String name;
        public final double protein;
        public final double kcal;
        public final double rupees;
        public final String unit;

        Food(String name, double protein, double kcal, double rupees, String unit) {
            this.name = name;
            this.protein = protein;
            this.kcal = kcal;
            this.rupees = rupees;
            this.unit = unit;
        }

        double factor(double qty) {
            return "pc".equals(unit) ? qty : qty / 100.0;
        }
    }

    static final Food SOYA = new Food("Soya chunks", 52, 345, 20, "g");
    static final Food ATTA = new Food("Wheat atta", 12, 340, 4, "g");
    static final Food RICE = new Food("Rice", 7, 350, 5, "g");
    static final Food DAL = new Food("Moong / masoor dal", 24, 340, 11, "g");
    static final Food CHANA = new Food("Kala chana", 19, 360, 9, "g");
    static final Food RAJMA = new Food("Rajma", 22, 330, 14, "g");
    static final Food PEANUT = new Food("Peanuts", 25, 570, 14, "g");
    static final Food BESAN = new Food("Besan", 22, 380, 10, "g");
    static final Food POHA = new Food("Poha", 7, 350, 6, "g");
    static final Food MILK = new Food("Toned milk", 3.2, 60, 5.6, "ml");
    static final Food CURD = new Food("Curd (homemade)", 3.5, 60, 6, "g");
    static final Food VEG = new Food("Seasonal sabzi", 2, 30, 4, "g");
    static final Food OIL = new Food("Cooking oil", 0, 900, 15, "ml");
    static final Food CHICKEN = new Food("Chicken (curry cut)", 16, 170, 22, "g");
    static final Food EGG = new Food("Egg", 6.3, 75, 7, "pc");
    static final Food BANANA = new Food("Banana", 1.3, 105, 6, "pc");

    /** Onion, tomato, ginger-garlic, spices and salt per day. */
    public static final double MASALA_RUPEES = 6;

    public static final Food[] PRICE_LIST = {
            SOYA, DAL, CHANA, RAJMA, PEANUT, BESAN, ATTA, RICE, POHA,
            MILK, CURD, EGG, CHICKEN, BANANA, VEG, OIL,
    };

    public static final class Item {
        public final String label;
        public final Food food;
        public final double qty;

        Item(String label, Food food, double qty) {
            this.label = label;
            this.food = food;
            this.qty = qty;
        }

        public double protein() {
            return food.protein * food.factor(qty);
        }

        public double kcal() {
            return food.kcal * food.factor(qty);
        }

        public double rupees() {
            return food.rupees * food.factor(qty);
        }

        public String amount() {
            String q = qty == Math.floor(qty) ? String.valueOf((int) qty) : String.valueOf(qty);
            return "pc".equals(food.unit) ? q : q + " " + food.unit;
        }
    }

    public static final class Meal {
        public final String time;
        public final String dish;
        public final Item[] items;

        Meal(String time, String dish, Item... items) {
            this.time = time;
            this.dish = dish;
            this.items = items;
        }

        public double protein() {
            double p = 0;
            for (Item i : items) p += i.protein();
            return p;
        }

        public double kcal() {
            double k = 0;
            for (Item i : items) k += i.kcal();
            return k;
        }

        public double rupees() {
            double r = 0;
            for (Item i : items) r += i.rupees();
            return r;
        }
    }

    public static final class Menu {
        public final String name;
        public final Meal[] meals;

        Menu(String name, Meal... meals) {
            this.name = name;
            this.meals = meals;
        }

        public double protein() {
            double p = 0;
            for (Meal m : meals) p += m.protein();
            return p;
        }

        public double kcal() {
            double k = 0;
            for (Meal m : meals) k += m.kcal();
            return k;
        }

        public double rupees() {
            double r = MASALA_RUPEES;
            for (Meal m : meals) r += m.rupees();
            return r;
        }
    }

    private Diet() {
    }

    private static Item it(String label, Food food, double qty) {
        return new Item(label, food, qty);
    }

    private static final Menu[] VEG_MENUS = {
            new Menu("Soya & dal day",
                    new Meal("Breakfast", "Besan chilla + milk",
                            it("Besan chilla (2)", BESAN, 60),
                            it("Milk", MILK, 250)),
                    new Meal("Lunch", "Dal, roti, rice, curd",
                            it("Roti (4)", ATTA, 120),
                            it("Moong dal", DAL, 60),
                            it("Rice", RICE, 80),
                            it("Curd", CURD, 150),
                            it("Sabzi", VEG, 100),
                            it("Oil", OIL, 10)),
                    new Meal("Evening snack", "Roasted chana + peanuts",
                            it("Roasted kala chana", CHANA, 50),
                            it("Peanuts", PEANUT, 25)),
                    new Meal("Dinner", "Soya chunk curry + roti",
                            it("Soya chunks (dry)", SOYA, 60),
                            it("Roti (3)", ATTA, 90),
                            it("Sabzi", VEG, 150),
                            it("Oil", OIL, 15))),
            new Menu("Rajma day",
                    new Meal("Breakfast", "Peanut poha + milk",
                            it("Poha", POHA, 60),
                            it("Peanuts", PEANUT, 20),
                            it("Milk", MILK, 250)),
                    new Meal("Lunch", "Rajma chawal + curd",
                            it("Rajma (dry)", RAJMA, 70),
                            it("Rice", RICE, 120),
                            it("Curd", CURD, 150),
                            it("Oil", OIL, 10)),
                    new Meal("Evening snack", "Soya chaat + banana",
                            it("Boiled soya chunks", SOYA, 40),
                            it("Banana", BANANA, 1)),
                    new Meal("Dinner", "Dal + soya bhurji + roti",
                            it("Masoor dal", DAL, 60),
                            it("Soya bhurji (dry)", SOYA, 40),
                            it("Roti (4)", ATTA, 120),
                            it("Sabzi", VEG, 150),
                            it("Oil", OIL, 15))),
            new Menu("Chana day",
                    new Meal("Breakfast", "Soya-stuffed paratha + curd",
                            it("Paratha (2)", ATTA, 60),
                            it("Soya filling (dry)", SOYA, 35),
                            it("Curd", CURD, 100)),
                    new Meal("Lunch", "Chana masala + rice",
                            it("Kala chana (dry)", CHANA, 80),
                            it("Rice", RICE, 120),
                            it("Sabzi", VEG, 100),
                            it("Oil", OIL, 10)),
                    new Meal("Evening snack", "Milk, peanuts, banana",
                            it("Milk", MILK, 250),
                            it("Peanuts", PEANUT, 30),
                            it("Banana", BANANA, 1)),
                    new Meal("Dinner", "Dal + soya curry + roti",
                            it("Moong dal", DAL, 70),
                            it("Soya chunks (dry)", SOYA, 35),
                            it("Roti (4)", ATTA, 120),
                            it("Sabzi", VEG, 100),
                            it("Oil", OIL, 15))),
    };

    private static final Menu[] NON_VEG_MENUS = {
            new Menu("Egg & soya day",
                    new Meal("Breakfast", "Egg bhurji + roti",
                            it("Eggs", EGG, 3),
                            it("Roti (2)", ATTA, 60),
                            it("Oil", OIL, 5)),
                    new Meal("Lunch", "Dal, rice, curd",
                            it("Moong dal", DAL, 60),
                            it("Rice", RICE, 120),
                            it("Curd", CURD, 150),
                            it("Sabzi", VEG, 100),
                            it("Oil", OIL, 10)),
                    new Meal("Evening snack", "Roasted chana + peanuts",
                            it("Roasted kala chana", CHANA, 50),
                            it("Peanuts", PEANUT, 20)),
                    new Meal("Dinner", "Soya chunk curry + roti",
                            it("Soya chunks (dry)", SOYA, 65),
                            it("Roti (4)", ATTA, 120),
                            it("Sabzi", VEG, 100),
                            it("Oil", OIL, 15))),
            new Menu("Chicken day",
                    new Meal("Breakfast", "Boiled eggs + roti",
                            it("Boiled eggs", EGG, 2),
                            it("Roti (2)", ATTA, 60),
                            it("Peanuts", PEANUT, 15)),
                    new Meal("Lunch", "Chicken curry + rice",
                            it("Chicken curry cut", CHICKEN, 150),
                            it("Rice", RICE, 150),
                            it("Oil", OIL, 10)),
                    new Meal("Evening snack", "Roasted chana",
                            it("Roasted kala chana", CHANA, 50)),
                    new Meal("Dinner", "Dal + soya curry + roti",
                            it("Masoor dal", DAL, 60),
                            it("Soya chunks (dry)", SOYA, 55),
                            it("Roti (4)", ATTA, 120),
                            it("Sabzi", VEG, 100),
                            it("Oil", OIL, 10))),
            new Menu("Egg curry day",
                    new Meal("Breakfast", "Masala omelette + roti",
                            it("Eggs", EGG, 3),
                            it("Roti (2)", ATTA, 60),
                            it("Oil", OIL, 5)),
                    new Meal("Lunch", "Rajma chawal + curd",
                            it("Rajma (dry)", RAJMA, 60),
                            it("Rice", RICE, 120),
                            it("Curd", CURD, 100),
                            it("Oil", OIL, 10)),
                    new Meal("Evening snack", "Soya chaat + peanuts",
                            it("Boiled soya chunks", SOYA, 50),
                            it("Peanuts", PEANUT, 20)),
                    new Meal("Dinner", "Egg curry + dal + roti",
                            it("Eggs", EGG, 2),
                            it("Moong dal", DAL, 50),
                            it("Roti (4)", ATTA, 120),
                            it("Sabzi", VEG, 100),
                            it("Oil", OIL, 15))),
    };

    /** Menus rotate every day so meals don't get boring. */
    public static Menu menu(int day, boolean veg) {
        Menu[] menus = veg ? VEG_MENUS : NON_VEG_MENUS;
        return menus[(day - 1) % menus.length];
    }

    public static final String[] TIPS = {
            "Soak chana and rajma overnight; pressure-cook a 2-day batch.",
            "Boil soya chunks 5 min and squeeze out the water before cooking.",
            "Set curd at home from the milk — it is cheaper than packet curd.",
            "Buy soya, dal, chana and peanuts in 1 kg packs to keep costs down.",
            "Weigh yourself weekly. Losing weight? Add 1 roti + 20 g peanuts. "
                    + "Gaining? Halve the rice at lunch.",
    };
}
