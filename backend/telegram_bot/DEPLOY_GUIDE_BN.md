# MyTube Telegram Bot ডিপ্লয়মেন্ট গাইড (বাংলায়)

এই বটের কাজ:
- আপনি টেলিগ্রাম বটের ইনবক্সে যেকোনো ভিডিও ফরোয়ার্ড করবেন।
- বটটি সাথে সাথে একটি Direct Stream & Download URL তৈরি করবে।
- সেই লিংকটি MyTube অ্যাপে "+" চেপে পেস্ট করলেই আপনি সরাসরি স্ট্রিম করতে এবং অ্যাপের মাধ্যমে ডাউনলোড করতে পারবেন!

---

### প্রয়োজনীয় তথ্য (Credentials) সংগ্রহ:

1. **Telegram API_ID & API_HASH:**
   - ব্রাউজারে [my.telegram.org](https://my.telegram.org) এ যান।
   - ফোন নম্বর দিয়ে লগইন করে **API development tools** এ ক্লিক করুন।
   - যেকোনো নাম দিয়ে একটি App তৈরি করুন এবং `API_ID` ও `API_HASH` কপি করুন।

2. **Telegram BOT_TOKEN:**
   - Telegram অ্যাপে `@BotFather` লিখে সার্চ করুন।
   - `/newbot` কমান্ড দিয়ে আপনার বটের নাম ও ইউজারনেম দিন।
   - BotFather আপনাকে একটি `BOT_TOKEN` দেবে।

3. **BIN_CHANNEL (স্টোরেজ চ্যানেল):**
   - টেলিগ্রামে একটি নতুন **Private Channel** তৈরি করুন।
   - সেখানে আপনার বটকে **Administrator** হিসেবে যুক্ত করুন।
   - চ্যানেলের ID বের করতে যেকোনো একটি ফরোয়ার্ডেড মেসেজ `@userinfobot` বা `@JsonDumpBot` এ দিন (যেমন: `-1001234567890`)।

---

### Render.com এ বিনামূল্যে (Free Tier) ডিপ্লয় করার ধাপ:

1. আপনার GitHub একাউন্টে এই রিপোজিটরিটি পুশ বা আপলোড করুন।
2. [Render.com](https://render.com) এ ফ্রি একাউন্ট খুলুন।
3. **New +** এ ক্লিক করে **Web Service** সিলেক্ট করুন।
4. আপনার GitHub রিপোজিটরিটি যুক্ত করুন।
5. সেটিংস দিন:
   - **Root Directory:** `backend/telegram_bot`
   - **Environment:** `Python 3`
   - **Build Command:** `pip install -r requirements.txt`
   - **Start Command:** `python bot.py`
6. **Environment Variables** সেকশনে গিয়ে নিচের ভ্যালুগুলো যোগ করুন:
   - `API_ID` = আপনার টেলিগ্রাম এপিআই আইডি
   - `API_HASH` = আপনার টেলিগ্রাম এপিআই হ্যাশ
   - `BOT_TOKEN` = আপনার বট টোকেন
   - `BIN_CHANNEL` = আপনার প্রাইভেট চ্যানেলের আইডি (যেমন: `-100...`)
   - `PORT` = `8080`
   - `SERVER_URL` = Render আপনাকে যে ফ্রি URL দেবে (যেমন: `https://mytube-bot.onrender.com`)
7. **Create Web Service** এ ক্লিক করুন। কয়েক মিনিটের মধ্যে বট লাইভ হয়ে যাবে!

---

### Free Tier এর সীমাবদ্ধতা ও বাস্তবতা:
- **Render Free Tier:** ১৫ মিনিট কোনো রিকোয়েস্ট না আসলে স্লিপ (Sleep Mode) এ যায়। নতুন রিকোয়েস্টে ঘুম ভাঙতে ৩০-৫০ সেকেন্ড লাগতে পারে।
- **RAM ও ফাইল সাইজ:** Render Free তে ৫১২ এমবি র‍্যাম থাকে। টেলিগ্রামের সাধারণ ভিডিও খুব সুন্দরভাবে চালায়।
