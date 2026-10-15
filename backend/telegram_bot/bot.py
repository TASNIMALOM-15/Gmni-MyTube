import os
import asyncio
import logging
from pyrogram import Client, filters
from pyrogram.types import Message
from aiohttp import web

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

API_ID = int(os.environ.get("API_ID", "0"))
API_HASH = os.environ.get("API_HASH", "")
BOT_TOKEN = os.environ.get("BOT_TOKEN", "")
BIN_CHANNEL = int(os.environ.get("BIN_CHANNEL", "0"))
PORT = int(os.environ.get("PORT", "8080"))
SERVER_URL = os.environ.get("SERVER_URL", "http://localhost:8080").rstrip("/")

app = Client(
    "MyTubeStreamBot",
    api_id=API_ID,
    api_hash=API_HASH,
    bot_token=BOT_TOKEN
)

# In-memory queue to process forwards sequentially
process_queue = asyncio.Queue()

async def queue_worker():
    while True:
        message = await process_queue.get()
        try:
            await handle_forwarded_video(message)
        except Exception as e:
            logger.error(f"Error processing video: {e}")
        finally:
            process_queue.task_done()

async def handle_forwarded_video(message: Message):
    status_msg = await message.reply_text("⏳ Processing video for MyTube... Forwarding to storage...")

    # Forward to permanent bin channel if configured
    if BIN_CHANNEL != 0:
        forwarded = await message.forward(BIN_CHANNEL)
        msg_id = forwarded.id
    else:
        msg_id = message.id

    stream_link = f"{SERVER_URL}/stream/{msg_id}"
    download_link = f"{SERVER_URL}/download/{msg_id}"

    text = (
        f"🎬 **MyTube Direct Link Generated!**\n\n"
        f"🔗 **Direct Stream URL:**\n`{stream_link}`\n\n"
        f"⬇️ **Direct Download URL:**\n`{download_link}`\n\n"
        f"💡 *Copy this link and paste in MyTube app '+' button to stream or download instantly!*"
    )
    await status_msg.edit_text(text)

@app.on_message(filters.command("start") & filters.private)
async def start_handler(client, message: Message):
    await message.reply_text(
        "👋 **স্বাগতম MyTube Bot-এ!**\n\n"
        "আমাকে যেকোনো Video বা File ফরোয়ার্ড করুন। আমি সাথে সাথে আপনার জন্য Direct Streaming ও Download লিংক তৈরি করে দেব, যা সরাসরি MyTube অ্যাপে চালানো ও ডাউনলোড করা যাবে।"
    )

@app.on_message((filters.video | filters.document) & filters.private)
async def media_handler(client, message: Message):
    await message.reply_text("📥 ভিডিও কিউতে যুক্ত হয়েছে। প্রসেসিং শুরু হচ্ছে...")
    await process_queue.put(message)

# Web Server for Streaming / Downloading
routes = web.RouteTableDef()

@routes.get("/")
async def root_handler(request):
    return web.Response(text="MyTube Telegram Bot & Stream Engine is Running!")

@routes.get("/health")
async def health_check(request):
    return web.json_response({"status": "healthy", "service": "mytube-bot"})

@routes.get("/stream/{id}")
@routes.get("/download/{id}")
async def stream_handler(request):
    msg_id = int(request.match_info["id"])
    target_chat = BIN_CHANNEL if BIN_CHANNEL != 0 else request.query.get("chat_id")

    try:
        msg = await app.get_messages(target_chat, msg_id)
        media = msg.video or msg.document
        if not media:
            return web.Response(status=404, text="Media not found")

        file_size = media.file_size
        mime_type = media.mime_type or "video/mp4"

        headers = {
            "Content-Type": mime_type,
            "Content-Length": str(file_size),
            "Accept-Ranges": "bytes"
        }

        # Check for HTTP Range Header
        range_header = request.headers.get("Range")
        offset = 0
        limit = file_size

        if range_header:
            from_byte, to_byte = range_header.replace("bytes=", "").split("-")
            offset = int(from_byte)
            if to_byte:
                limit = int(to_byte) - offset + 1
            headers["Content-Range"] = f"bytes {offset}-{offset + limit - 1}/{file_size}"
            headers["Content-Length"] = str(limit)

        response = web.StreamResponse(
            status=206 if range_header else 200,
            headers=headers
        )
        await response.prepare(request)

        async for chunk in app.stream_media(msg, offset=offset, limit=limit):
            await response.write(chunk)

        await response.write_eof()
        return response

    except Exception as e:
        logger.error(f"Stream error: {e}")
        return web.Response(status=500, text=f"Error: {e}")

async def start_web_server():
    server = web.Application()
    server.add_routes(routes)
    runner = web.AppRunner(server)
    await runner.setup()
    site = web.TCPSite(runner, "0.0.0.0", PORT)
    await site.start()
    logger.info(f"Server started on port {PORT}")

async def main():
    asyncio.create_task(queue_worker())
    await start_web_server()
    await app.start()
    logger.info("Bot started successfully!")
    await asyncio.Event().wait()

if __name__ == "__main__":
    asyncio.run(main())
